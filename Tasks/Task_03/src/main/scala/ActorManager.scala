import akka.actor.typed.receptionist.Receptionist.{Find, Listing}
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior, Terminated}
import akka.util.Timeout

import scala.collection.immutable.HashMap
import scala.concurrent.duration.DurationInt
import scala.util.Success

object ActorManager {
    val serviceKey: ServiceKey[ActorManagerProtocol] =
        ServiceKey[ActorManagerProtocol]("actorManager")

    trait ActorManagerProtocol

    object SetupActorManager extends ActorManagerProtocol

    object TerminateSystem extends ActorManagerProtocol

    case class InitializeSystemWithFilePath(filePath: String)
        extends ActorManagerProtocol

    case class SendFilePathToFileParseActor(
        convertDataActorRef: ActorRef[ParseFileActor.ParseFileActorProtocol],
        filePath: String
    ) extends ActorManagerProtocol

    def apply(): Behavior[ActorManagerProtocol] = {
        Behaviors
            .setup[ActorManagerProtocol] { context =>
                implicit val timeout: Timeout = 3.seconds

                Behaviors
                    .receive[ActorManagerProtocol] {
                        (context, message: ActorManagerProtocol) =>
                            message match {
                                case TerminateSystem =>
                                    Behaviors.stopped

                                case InitializeSystemWithFilePath(filePath) =>
                                    // Maybe refactor into helper method or use something else entirely
                                    // In any way, seems wrong to do it this way...
                                    context.ask(
                                      context.system.receptionist,
                                      Find(ParseFileActor.serviceKey)
                                    ) { case Success(listing: Listing) =>
                                        val instances =
                                            listing.serviceInstances(
                                              ParseFileActor.serviceKey
                                            )
                                        val parseFileActorRef =
                                            instances.iterator.next()

                                        SendFilePathToFileParseActor(
                                          parseFileActorRef,
                                          filePath
                                        )
                                    }

                                    Behaviors.same;
                                case SendFilePathToFileParseActor(
                                      parseFileActorRef,
                                      filePath
                                    ) =>
                                    parseFileActorRef ! ParseFileActor
                                        .LoadDataFromFileAndGetParseActor(
                                          filePath
                                        )

                                    Behaviors.same;
                                case SetupActorManager =>
                                    context.log.info("Spawning Actors...")
                                    val fileParserActor =
                                        context.spawn(
                                          ParseFileActor(),
                                          "fileParserActor"
                                        )
                                    context.system.receptionist ! Receptionist
                                        .register(
                                          ParseFileActor.serviceKey,
                                          fileParserActor
                                        )

                                    val dataConverterActor =
                                        context.spawn(
                                          ConvertDataActor(),
                                          "dataConverterActor"
                                        )
                                    context.system.receptionist ! Receptionist
                                        .register(
                                          ConvertDataActor.serviceKey,
                                          dataConverterActor
                                        )

                                    val averagerActor =
                                        context.spawn(
                                          AveragerActor(
                                            Option(HashMap[String, Seq[Tick]]())
                                          ),
                                          "averagerActor"
                                        )
                                    context.system.receptionist ! Receptionist
                                        .register(
                                          AveragerActor.serviceKey,
                                          averagerActor
                                        )

                                    val databaseConnectorActor = context.spawn(
                                      DatabaseConnectorActor(),
                                      "databaseConnectorActor"
                                    )
                                    context.system.receptionist ! Receptionist
                                        .register(
                                          DatabaseConnectorActor.serviceKey,
                                          databaseConnectorActor
                                        )

                                    // https://doc.akka.io/docs/akka/current/typed/actor-lifecycle.html#watching-actors
                                    context.watch(databaseConnectorActor)

                                    context.log.info(
                                      "All actors created successfully!"
                                    )

                                    Behaviors.same;
                            }
                    }
                    .receiveSignal { case (context, Terminated(ref)) =>
                        Behaviors.stopped
                    }
            }

    }
}
