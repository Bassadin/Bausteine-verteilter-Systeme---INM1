import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors

object ToUpper {
    def apply(): Behavior[String] = {
        Behaviors.receive((context, message) => {
            println(message.toUpperCase)
            Behaviors.same
        })
    }
}
