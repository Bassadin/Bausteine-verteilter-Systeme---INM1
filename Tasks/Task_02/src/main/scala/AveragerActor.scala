import akka.actor.typed.ActorRef

trait AveragerActorProtocol

object EndAveragerActor extends AveragerActorProtocol
case class AveragerTickData(newTick: Tick) extends AveragerActorProtocol

object AveragerActor {}
