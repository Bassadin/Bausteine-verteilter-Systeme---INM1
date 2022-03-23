import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors

trait ToUpperProtocol
object End extends ToUpperProtocol
case class Text(content: String) extends ToUpperProtocol

object ToUpper {
    def apply(): Behavior[ToUpperProtocol] = {
        Behaviors.receive((context, message) => {
            message match {
                case End =>
                    context.log.info("Terminating actor...")
                    Behaviors.stopped
                case Text(content) =>
                    context.log.info(content.toUpperCase())
                    Behaviors.same
            }
        })
    }
}
