import ConvertDataActor.{ConvertDataActorProtocol, ListingResponse}
import DatabaseConnectorActor.{DatabaseConnectorActorProtocol, ListingResponse}
import akka.actor.typed.SupervisorStrategy.Stop

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
    case class ParseFile(csvPath: String) extends ParseFileActorProtocol

    case class ListingResponse(listing: Receptionist.Listing)
        extends ParseFileActorProtocol

    val serviceKey: ServiceKey[ParseFileActorProtocol] =
        ServiceKey[ParseFileActorProtocol]("fileParser")

    def apply(csvPath: String): Behavior[ParseFileActorProtocol] =
        Behaviors.setup { context =>
            context.log.info("--- Parse File Actor UP ---")

            context.system.receptionist ! Receptionist.register(
              this.serviceKey,
              context.self
            );

            context.log.info("Trying to send SendFileDataToConvertActor")
            context.self ! ParseFile(csvPath);

            val subscriptionAdapter =
                context.messageAdapter[Receptionist.Listing](
                  ListingResponse.apply
                )

            context.system.receptionist ! Receptionist.Subscribe(
              ConvertDataActor.serviceKey,
              subscriptionAdapter
            )

            Behaviors.receiveMessage {

                case ListingResponse(
                      ConvertDataActor.serviceKey.Listing(listings)
                    ) =>
                    listings.headOption match {
                        case Some(converterRef) =>
                            context.log.info(
                              "Using converter ref {}",
                              converterRef
                            )
                            handleConverterRef(converterRef)
                        case None =>
                            Behaviors.same
                    };
                case ParseFile(csvPath) =>
                    context.self ! ParseFile(csvPath);
                    Behaviors.same;
            }
        }

    private def handleConverterRef(
        converterActorRef: ActorRef[ConvertDataActor.ConvertDataActorProtocol]
    ): Behavior[ParseFileActorProtocol] = Behaviors.setup { context =>
        Behaviors.receiveMessage { case ParseFile(csvPath) =>
            context.log.info("Received message SendFileDataToConvertActor")

            // https://alvinalexander.com/scala/how-to-open-read-text-files-in-scala-cookbook-examples/
            // Drop first 4 lines since they're just headers
            for (line <- Source.fromFile(csvPath).getLines.drop(4)) {
                converterActorRef ! ConvertDataActor
                    .HandleFileLineString(line)
            }

            Behaviors.same;
        }
    }
}
