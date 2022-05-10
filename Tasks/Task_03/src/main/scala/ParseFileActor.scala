import ConvertDataActor.ConvertDataActorProtocol

import scala.io.Source
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.receptionist.Receptionist.{Find, Listing}
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.util.Timeout

import java.util.concurrent.TimeUnit
import scala.util.Success

object ParseFileActor {
    trait ParseFileActorProtocol extends MySerializable

    object TerminateParseFileActor extends ParseFileActorProtocol
    case class TerminateParseFileActorWithNextActorRef(
        parseFileActorRef: ActorRef[ConvertDataActorProtocol]
    ) extends ParseFileActorProtocol

    case class LoadDataFromFileAndGetParseActor(newData: String)
        extends ParseFileActorProtocol
    case class SendFileDataToConvertActor(
        convertDataActorRef: ActorRef[ConvertDataActorProtocol],
        newData: String
    ) extends ParseFileActorProtocol

    val serviceKey: ServiceKey[ParseFileActorProtocol] =
        ServiceKey[ParseFileActorProtocol]("fileParser")

    def apply(): Behavior[ParseFileActorProtocol] = Behaviors.setup { context =>

        context.log.info("--- Parse File Actor UP ---")

        implicit val timeout: Timeout =
            Timeout.apply(100, TimeUnit.MILLISECONDS)

        Behaviors.receiveMessagePartial {
            case TerminateParseFileActor =>
                context.ask(
                  context.system.receptionist,
                  Receptionist.Find(ConvertDataActor.serviceKey)
                ) { case Success(listing) =>
                    val instances = listing.serviceInstances(
                      ConvertDataActor.serviceKey
                    )
                    val convertDataActorReference = instances.iterator.next()
                    TerminateParseFileActorWithNextActorRef(
                      convertDataActorReference
                    )
                }
                Behaviors.same;

            case TerminateParseFileActorWithNextActorRef(
                  convertDataActorReference
                ) =>
                context.system.receptionist ! Receptionist.Deregister(
                  this.serviceKey,
                  context.self
                )
                convertDataActorReference ! ConvertDataActor.TerminateConvertDataActor
                Behaviors.stopped

            case LoadDataFromFileAndGetParseActor(newData) =>
                context.log.info(
                  "Valid file path: " + newData + ". Now parsing..."
                )

                context.ask(
                  context.system.receptionist,
                  Find(ConvertDataActor.serviceKey)
                ) { case Success(listing: Listing) =>
                    val instances =
                        listing.serviceInstances(
                          ConvertDataActor.serviceKey
                        )
                    val convertDataActorRef = instances.head

                    SendFileDataToConvertActor(
                      convertDataActorRef,
                      newData
                    )

                }
                Behaviors.same;
            case SendFileDataToConvertActor(convertDataActorRef, newData) =>
                // https://alvinalexander.com/scala/how-to-open-read-text-files-in-scala-cookbook-examples/
                // Drop first 4 lines since they're just headers
                for (line <- Source.fromFile(newData).getLines.drop(4)) {
                    convertDataActorRef ! ConvertDataActor
                        .SendDataToConvertAndFindDBActor(line)
                }

                convertDataActorRef ! ConvertDataActor.TerminateConvertDataActor
                context.self ! this.TerminateParseFileActor

                Behaviors.same;
        }
    }
}
