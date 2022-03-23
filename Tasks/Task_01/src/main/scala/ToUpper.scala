import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors

object ToUpper {
    def apply(): Behavior[String] = {
        Behaviors.receive((context, message) => {
            message match {
                case "" =>
                    println("Terminating actor...")
                    Behaviors.stopped
                case text =>
                    println(message.toUpperCase)
                    Behaviors.same
            }
        })
    }
}
