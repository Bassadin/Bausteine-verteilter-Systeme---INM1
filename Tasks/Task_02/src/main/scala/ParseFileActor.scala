import scala.io.Source
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.receptionist.Receptionist.{Find, Listing}
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.util.Timeout

import java.util.concurrent.TimeUnit
import scala.util.Success

trait ParseFileActorProtocol
object StopParseFileActor extends ParseFileActorProtocol;
case class LoadDataFromFileAndGetParseActor(newData: String)
    extends ParseFileActorProtocol;
case class SendFileDataToConvertActor(
    convertDataActorRef: ActorRef[ConvertDataActorProtocol],
    newData: String
) extends ParseFileActorProtocol;
private case class ListingResponse(listing: Receptionist.Listing)
    extends ParseFileActorProtocol

object ParseFileActor {
    val serviceKey = ServiceKey[ParseFileActorProtocol]("fileParser");

    def apply(): Behavior[ParseFileActorProtocol] = Behaviors.setup { context =>
        context.log.info("ParseFileActor Setup call")

        implicit val timeout: Timeout =
            Timeout.apply(100, TimeUnit.MILLISECONDS)

        Behaviors.receiveMessagePartial {
            case StopParseFileActor =>
                context.log.info("Terminating parse file actor...")
                Behaviors.stopped;
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
                    val convertDataActorRef = instances.iterator.next()

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
                    convertDataActorRef ! DataToConvert(line);
                }

                // Quit the convert data actor afterwards
                //                    convertDataActorRef ! EndConvertDataActor;
                Behaviors.same;
        }
    }
}
