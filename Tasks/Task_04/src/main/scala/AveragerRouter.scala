import AveragerActor.{HandleNewTickData, ListingResponse, handleDBRef}
import ConvertDataActor.ConvertDataActorProtocol
import akka.actor.typed.SupervisorStrategy
import akka.actor.typed.receptionist.ServiceKey
import akka.actor.typed.scaladsl.{Behaviors, Routers}

object AveragerRouter {

    sealed trait AveragerRouterProtocol;

    case class handleTickData(newTick: Tick) extends AveragerRouterProtocol;

    val serviceKey = ServiceKey[AveragerRouterProtocol]("averagerRouter")

    Behaviors.setup[Unit] { ctx =>
        val pool = Routers.pool(poolSize = 10) {
            // make sure the workers are restarted if they fail
            Behaviors.supervise(AveragerActor()).onFailure[Exception](SupervisorStrategy.restart)
        }
        val router = ctx.spawn(pool, "worker-pool")

        Behaviors.receiveMessagePartial { case handleTickData(newTick: Tick) =>
            router ! AveragerActor.HandleNewTickData(newTick);
            Behaviors.empty
        }
    }
}
