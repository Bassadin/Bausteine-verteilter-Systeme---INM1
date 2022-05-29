import AveragerActor.{AveragerActorProtocol, HandleNewTickData, ListingResponse, handleDBRef}
import akka.actor.typed.{ActorRef, Behavior, SupervisorStrategy}
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.{Behaviors, Routers}

object AveragerRouter {

    sealed trait AveragerRouterProtocol extends ActorProtocolSerializable

    case class HandleTickData(newTick: Tick) extends AveragerRouterProtocol
    case class Terminate() extends AveragerRouterProtocol
    case class ListingResponse(listing: Receptionist.Listing) extends AveragerRouterProtocol

    val serviceKey = ServiceKey[AveragerRouterProtocol]("averagerRouter")

    def apply(): Behavior[AveragerRouterProtocol] = {
        Behaviors.setup[AveragerRouterProtocol] { context =>
            context.log.info("AveragerRouter - starting group")
            context.system.receptionist ! Receptionist.register(this.serviceKey, context.self)

            val group = Routers.group(AveragerActor.serviceKey)
            val groupRouter = context.spawn(group.withConsistentHashingRouting(1, _.symbolIdentifier), "AveragerRouter")

            // Subscription to db actor
            context.system.receptionist ! Receptionist.register(this.serviceKey, context.self)
            val subscriptionAdapter = context.messageAdapter[Receptionist.Listing](ListingResponse.apply)

            context.system.receptionist ! Receptionist.Subscribe(DatabaseConnectorActor.serviceKey, subscriptionAdapter)

            context.log.info("AveragerRouter - starting receiveMessagePartial")

            Behaviors.receiveMessagePartial {
                case ListingResponse(DatabaseConnectorActor.serviceKey.Listing(listings)) =>
                    listings.headOption match {
                        case Some(dbActorRef) =>
                            context.log.info("Using db actor ref {}", dbActorRef)
                            handleDBRef(dbActorRef, groupRouter)
                        case None =>
                            Behaviors.same
                    }
            }
        }
    }

    private def handleDBRef(
        dbActorRef: ActorRef[DatabaseConnectorActor.DatabaseConnectorActorProtocol],
        groupRouter: ActorRef[AveragerActor.AveragerActorProtocol]
    ): Behavior[AveragerRouterProtocol] = Behaviors.setup { context =>
        context.log.info("Handledbref setup call")

        val subscriptionAdapter = context.messageAdapter[Receptionist.Listing](ListingResponse.apply)

        context.system.receptionist ! Receptionist.Subscribe(AveragerActor.serviceKey, subscriptionAdapter)

        Behaviors.receiveMessagePartial { case ListingResponse(AveragerActor.serviceKey.Listing(listings)) =>
            context.log.info("got averager actors ref list")
            handleAveragerActorsListForTermination(dbActorRef, listings, groupRouter)

        }
    }

    private def handleAveragerActorsListForTermination(
        dbActorRef: ActorRef[DatabaseConnectorActor.DatabaseConnectorActorProtocol],
        averagerListings: Set[ActorRef[AveragerActor.AveragerActorProtocol]],
        groupRouter: ActorRef[AveragerActor.AveragerActorProtocol]
    ): Behavior[AveragerRouterProtocol] = Behaviors.setup { context =>
        context.log.info("handleAveragerActorsListForTermination setup call")

        Behaviors.receiveMessagePartial {
            case this.Terminate() =>
                context.system.receptionist ! Receptionist.Deregister(this.serviceKey, context.self)
                context.log.info("Terminating averager actors")
                terminateAveragersWithBroadcast(averagerListings)
                dbActorRef ! DatabaseConnectorActor.Terminate()
                Behaviors.stopped
            case HandleTickData(newTick) =>
                groupRouter ! AveragerActor.HandleNewTickData(newTick)
                Behaviors.same

        }
    }

    private def terminateAveragersWithBroadcast(
        averagerListing: Set[ActorRef[AveragerActor.AveragerActorProtocol]]
    ): Unit = {
        for (eachAverager: ActorRef[AveragerActor.AveragerActorProtocol] <- averagerListing) {
            eachAverager ! AveragerActor.Terminate()
        }
    }

}
