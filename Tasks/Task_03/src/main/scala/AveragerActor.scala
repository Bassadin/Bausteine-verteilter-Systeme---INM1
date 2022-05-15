import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}

import java.time.Duration
import scala.collection.immutable.HashMap

object AveragerActor {
    trait AveragerActorProtocol extends ActorProtocolSerializable

    case class HandleNewTickData(
        newTick: Tick
    ) extends AveragerActorProtocol

    case class ListingResponse(listing: Receptionist.Listing)
        extends AveragerActorProtocol

    case class Terminate() extends AveragerActorProtocol

    val serviceKey: ServiceKey[AveragerActorProtocol] =
        ServiceKey[AveragerActorProtocol]("averagerDataActor")

    // Get average of a Tick Seq
    def averagePriceOfTicks(tickList: Seq[Tick]): Long = {
        val priceList: Seq[Long] = tickList.map(_.price)
        val tickPriceAverage: Long = priceList.sum / priceList.length
        tickPriceAverage
    }

    // https://www.youtube.com/watch?v=gwZjdRQTPu8
    // https://www.baeldung.com/scala/option-type
    def handleNewTickDataForAveraging(
        symbolToTicksMap: Map[String, Seq[Tick]],
        newTick: Tick,
        dbActorRef: ActorRef[
          DatabaseConnectorActor.DatabaseConnectorActorProtocol
        ]
    ): Behavior[AveragerActorProtocol] = {
        if (symbolToTicksMap.isEmpty) {
            handleDBRef(dbActorRef, HashMap[String, Seq[Tick]]())
        }

        // https://alvinalexander.com/scala/how-to-add-update-remove-elements-immutable-maps-scala/
        if (!symbolToTicksMap.contains(newTick.symbol)) {
            val mapWithNewEmptySequence =
                symbolToTicksMap + (newTick.symbol -> Seq[Tick]())
            handleDBRef(dbActorRef, mapWithNewEmptySequence)
        } else {

            val tickSeqForSymbol: Seq[Tick] =
                symbolToTicksMap(newTick.symbol)

            /*
            If time difference between all entries for a single symbol and the new
            tick are within 5 mins, add the new tick to the existing seq.
            Otherwise, send to db actor and initialize new seq
             */
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
                    symbolToTicksMap + (newTick.symbol -> seqWithNewValue)
                handleDBRef(dbActorRef, mapWithNewSeqIncludingNewValue)
            } else {
                // Replace existing seq
                val averagedTickDataForExistingSeq: Tick = Tick(
                  newTick.symbol,
                  tickSeqForSymbol.head.timestamp,
                  averagePriceOfTicks(tickSeqForSymbol)
                )

                dbActorRef ! DatabaseConnectorActor.HandleAveragedTickData(
                  averagedTickDataForExistingSeq
                )

                val mapWithNewEmptySeq =
                    symbolToTicksMap +
                        (newTick.symbol -> (Seq[Tick]() :+ newTick))

                handleDBRef(dbActorRef, mapWithNewEmptySeq)
            }
        }
    }

    def apply(): Behavior[AveragerActorProtocol] = {
        Behaviors
            .setup[AveragerActorProtocol] { context =>
                context.system.receptionist ! Receptionist.register(
                  this.serviceKey,
                  context.self
                )

                context.log.info("--- Averager Actor UP ---")

                val subscriptionAdapter =
                    context.messageAdapter[Receptionist.Listing](
                      ListingResponse.apply
                    )

                context.system.receptionist ! Receptionist.Subscribe(
                  DatabaseConnectorActor.serviceKey,
                  subscriptionAdapter
                )

                Behaviors.receiveMessagePartial {

                    case ListingResponse(
                          DatabaseConnectorActor.serviceKey.Listing(listings)
                        ) =>
                        listings.headOption match {
                            case Some(dbActorRef) =>
                                context.log.info(
                                  "Using dbActorRef {}",
                                  dbActorRef
                                )
                                handleDBRef(
                                  dbActorRef,
                                  Map[String, Seq[Tick]]()
                                )
                            case None =>
                                Behaviors.same
                        }
                    case HandleNewTickData(newTick) =>
                        context.self ! HandleNewTickData(newTick)
                        Behaviors.same;
                }
            }
    }

    private def handleDBRef(
        dbActorRef: ActorRef[
          DatabaseConnectorActor.DatabaseConnectorActorProtocol
        ],
        symbolToTicksMap: Map[String, Seq[Tick]]
    ): Behavior[AveragerActorProtocol] = Behaviors.setup { context =>
        Behaviors.receiveMessagePartial {
            case HandleNewTickData(newTick) =>
                this.handleNewTickDataForAveraging(
                  symbolToTicksMap,
                  newTick,
                  dbActorRef
                )
            case this.Terminate() =>
                context.log.info("Terminating Averager Actor")
                dbActorRef ! DatabaseConnectorActor.Terminate();
                Behaviors.stopped
        }
    }

}
