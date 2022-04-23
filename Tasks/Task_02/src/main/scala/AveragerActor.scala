import akka.actor.typed.receptionist.Receptionist.{Find, Listing}
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import akka.util.Timeout

import java.time.Duration
import scala.collection.immutable.HashMap
import scala.concurrent.duration.DurationInt
import scala.util.Success

object AveragerActor {
    trait AveragerActorProtocol

    object TerminateAveragerActor extends AveragerActorProtocol;
    case class TerminateAveragerActorWithNextActorRef(
        databaseConnectorActorReference: ActorRef[
          DatabaseConnectorActor.DatabaseConnectorActorProtocol
        ]
    ) extends AveragerActorProtocol;

    case class GetDBActorRefAndSendAveragerTickData(newTick: Tick)
        extends AveragerActorProtocol
    case class SendAveragerTickDataToDBActor(
        dbActorRef: ActorRef[
          DatabaseConnectorActor.DatabaseConnectorActorProtocol
        ],
        newTick: Tick
    ) extends AveragerActorProtocol
    val serviceKey: ServiceKey[AveragerActorProtocol] =
        ServiceKey[AveragerActorProtocol]("averagerDataActor")

    def averagePriceOfTicks(tickList: Seq[Tick]): Long = {
        val priceList: Seq[Long] = tickList.map(_.price)
        val tickPriceAverage: Long = priceList.sum / priceList.length
        tickPriceAverage
    }

    // https://www.youtube.com/watch?v=gwZjdRQTPu8
    // https://www.baeldung.com/scala/option-type
    def handleNewTickDataForAveraging(
        symbolToTicksMap: Option[Map[String, Seq[Tick]]],
        newTick: Tick,
        dbActorRef: ActorRef[
          DatabaseConnectorActor.DatabaseConnectorActorProtocol
        ]
    ): Behavior[AveragerActorProtocol] = {

        if (symbolToTicksMap.isEmpty) {
            return this.apply(Option(HashMap[String, Seq[Tick]]()))
        }

        // https://alvinalexander.com/scala/how-to-add-update-remove-elements-immutable-maps-scala/
        if (!symbolToTicksMap.get.contains(newTick.symbol)) {
            val mapWithNewEmptySequence =
                symbolToTicksMap.get + (newTick.symbol -> Seq[Tick]())
            this.apply(Option(mapWithNewEmptySequence))
        } else {

            val tickSeqForSymbol: Seq[Tick] =
                symbolToTicksMap.get(newTick.symbol)

            if (
              tickSeqForSymbol.forall(tick =>
                  Duration
                      .between(tick.timestamp, newTick.timestamp)
                      .toMinutes <= 5
              )
            ) {
                // Add to existing seq
                val seqWithNewValue: Seq[Tick] = tickSeqForSymbol :+ newTick
                val mapWithNewSeqIncludingNewValue =
                    symbolToTicksMap.get + (newTick.symbol -> seqWithNewValue)
                this.apply(Option(mapWithNewSeqIncludingNewValue))
            } else {
                // Replace existing seq

                dbActorRef ! DatabaseConnectorActor.AveragerTickData(
                  Tick(
                    newTick.symbol,
                    tickSeqForSymbol.head.timestamp,
                    averagePriceOfTicks(tickSeqForSymbol)
                  )
                )

                val mapWithNewEmptySeq =
                    symbolToTicksMap.get + (newTick.symbol -> (Seq[
                      Tick
                    ]() :+ newTick))
                this.apply(Option(mapWithNewEmptySeq))
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
                    case TerminateAveragerActor =>
                        context.ask(
                          context.system.receptionist,
                          Receptionist.Find(DatabaseConnectorActor.serviceKey)
                        ) { case Success(listing) =>
                            val instances = listing.serviceInstances(
                              DatabaseConnectorActor.serviceKey
                            )
                            val databaseConnectorActorReference =
                                instances.iterator.next()
                            TerminateAveragerActorWithNextActorRef(
                              databaseConnectorActorReference
                            )
                        }
                        Behaviors.same;

                    case TerminateAveragerActorWithNextActorRef(
                          databaseConnectorActorReference
                        ) =>
                        // https://stackoverflow.com/a/8610807/3526350
                        symbolToTicksMap.get.foreach { case (symbol, ticks) =>
                            if (!ticks.isEmpty) {
                                databaseConnectorActorReference ! DatabaseConnectorActor
                                    .AveragerTickData(
                                      Tick(
                                        symbol,
                                        ticks.head.timestamp,
                                        averagePriceOfTicks(ticks)
                                      )
                                    )
                            }
                        }

                        context.system.receptionist ! Receptionist.Deregister(
                          this.serviceKey,
                          context.self
                        )
                        databaseConnectorActorReference ! DatabaseConnectorActor.TerminateDatabaseConnectorActor
                        Behaviors.stopped

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
                          dbActorRef,
                          newTick
                        ) =>
                        handleNewTickDataForAveraging(
                          symbolToTicksMap,
                          newTick,
                          dbActorRef
                        );
                }
            }
    }
}
