import akka.actor.typed.receptionist.Receptionist.{Find, Listing}
import akka.actor.typed.receptionist.ServiceKey
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import akka.util.Timeout

import java.time.Duration
import scala.collection.immutable.HashMap
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

    // https://www.youtube.com/watch?v=gwZjdRQTPu8
    // https://www.baeldung.com/scala/option-type
    def handleNewTickDataForAveraging(
        symbolToTicksMap: Option[Map[String, Seq[Tick]]],
        newTick: Tick
    ): Behavior[AveragerActorProtocol] = {

        if (symbolToTicksMap.isEmpty) {
            return this.apply(Option(HashMap[String, Seq[Tick]]()));
        }

        // https://alvinalexander.com/scala/how-to-add-update-remove-elements-immutable-maps-scala/
        if (!symbolToTicksMap.get.contains(newTick.symbol)) {
            val mapWithNewEmptySequence = symbolToTicksMap.get + (newTick.symbol -> Seq[Tick]());
            this.apply(Option(mapWithNewEmptySequence));
        } else {

            val tickSeqForSymbol: Seq[Tick] = symbolToTicksMap.get(newTick.symbol);

            if (tickSeqForSymbol.forall(tick => Duration.between(tick.timestamp, newTick.timestamp).toMinutes <= 5 )) {
                val seqWithNewValue: Seq[Tick] = tickSeqForSymbol :+ newTick;
                val mapWithNewSeqIncludingNewValue = symbolToTicksMap.get + (newTick.symbol -> seqWithNewValue);
                this.apply(Option(mapWithNewSeqIncludingNewValue));
            } else {
                
            }

        }
    }

    def apply(
        symbolToTicksMap: Option[Map[String, Seq[Tick]]]
    ): Behavior[AveragerActorProtocol] = {
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
                        handleNewTickDataForAveraging(
                          symbolToTicksMap,
                          newTick
                        );
                }
            }
    }
}
