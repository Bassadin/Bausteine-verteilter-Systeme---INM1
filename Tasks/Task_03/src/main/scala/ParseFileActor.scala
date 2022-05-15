import ConvertDataActor.{Terminate, WorkReady}
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}

import scala.io.{BufferedSource, Source}

object ParseFileActor {
    trait ParseFileActorProtocol extends ActorProtocolSerializable
    case class ParseFile(csvPath: String) extends ParseFileActorProtocol

    case class ListingResponse(listing: Receptionist.Listing)
        extends ParseFileActorProtocol

    case class AskForWork(
        converterRef: ActorRef[ConvertDataActor.ConvertDataActorProtocol]
    ) extends ParseFileActorProtocol

    val serviceKey: ServiceKey[ParseFileActorProtocol] =
        ServiceKey[ParseFileActorProtocol]("fileParser")

    def apply(csvPath: String): Behavior[ParseFileActorProtocol] =
        Behaviors.setup { context =>
            context.log.info("--- Parse File Actor UP ---")

            context.system.receptionist ! Receptionist.register(
              this.serviceKey,
              context.self
            )

            context.log.info("Trying to send SendFileDataToConvertActor")
            context.self ! ParseFile(csvPath)

            val subscriptionAdapter =
                context.messageAdapter[Receptionist.Listing](
                  ListingResponse.apply
                )

            context.system.receptionist ! Receptionist.Subscribe(
              ConvertDataActor.serviceKey,
              subscriptionAdapter
            )

            Behaviors.receiveMessagePartial {

                case ListingResponse(
                      ConvertDataActor.serviceKey.Listing(listings)
                    ) =>
                    // https://www.garysieling.com/blog/scala-headoption-example/
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
                    context.self ! ParseFile(csvPath)
                    Behaviors.same;
            }
        }

    private def handleConverterRef(
        converterActorRef: ActorRef[ConvertDataActor.ConvertDataActorProtocol]
    ): Behavior[ParseFileActorProtocol] = Behaviors.setup { context =>
        Behaviors.receiveMessagePartial {
            case ParseFile(csvPath) =>
                context.log.info("Received message SendFileDataToConvertActor")

                // https://alvinalexander.com/scala/how-to-open-read-text-files-in-scala-cookbook-examples/
                // Drop first 4 lines since they're just headers
                val bufferedReader = Source.fromFile(csvPath);
                val batches = bufferedReader.getLines
                    .drop(4)
                    .grouped(100)

                context.log.info("Initialize work pulling behavior")
                converterActorRef ! ConvertDataActor.WorkReady(context.self)
                this.workPullingBehavior(
                  batches,
                  bufferedReader
                )
            case AskForWork(converterRef) =>
                context.self ! AskForWork(converterRef);
                Behaviors.same

        }
    }

    private def workPullingBehavior(
        groupedIterator: Iterator[String]#GroupedIterator[String] = null,
        bufferedReader: BufferedSource = null
    ): Behavior[ParseFileActorProtocol] = Behaviors.setup { context =>
        Behaviors.receiveMessagePartial { case AskForWork(converterRef) =>
            if (groupedIterator.hasNext) {
                converterRef ! ConvertDataActor.HandleFileBatchedLines(
                  groupedIterator.next,
                  context.self
                )
            } else {
                converterRef ! Terminate();
                bufferedReader.close;
            }
            Behaviors.same
        }
    }
}
