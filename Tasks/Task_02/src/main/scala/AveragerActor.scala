import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.receptionist.Receptionist.{Find, Listing}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import akka.util.Timeout

import scala.concurrent.duration.DurationInt
import scala.util.Success

trait AveragerActorProtocol

object EndAveragerActor extends AveragerActorProtocol
case class GetDBActorRefAndSendAveragerTickData(newTick: Tick)
    extends AveragerActorProtocol
case class SendAveragerTickDataToDBActor(
    dbActorRef: ActorRef[DatabaseConnectorActorProtocol],
    newTick: Tick
) extends AveragerActorProtocol

object AveragerActor {
    val serviceKey: ServiceKey[AveragerActorProtocol] =
        ServiceKey[AveragerActorProtocol]("averagerDataActor")

    def apply(): Behavior[AveragerActorProtocol] = {
        Behaviors
            .setup[AveragerActorProtocol] { context =>
                implicit val timeout: Timeout = 3.seconds

                Behaviors.receiveMessage {
                    case GetDBActorRefAndSendAveragerTickData(newTick) =>
                        context.ask(
                          context.system.receptionist,
                          Find(DatabaseConnectorActor.serviceKey)
                        ) { case Success(listing: Listing) =>
                            val instances =
                                listing.serviceInstances(
                                  DatabaseConnectorActor.serviceKey
                                )
                            val parseFileActorRef =
                                instances.iterator.next()

                            SendAveragerTickDataToDBActor(
                              parseFileActorRef,
                              newTick
                            )
                        }

                        Behaviors.same;
                    case SendAveragerTickDataToDBActor(
                          parseFileActorRef,
                          newTick
                        ) =>
                        parseFileActorRef ! TickData(
                          newTick
                        );

                        Behaviors.same;
                }
            }
    }
}
