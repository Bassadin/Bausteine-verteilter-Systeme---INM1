import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.ActorSystem
class convertDataActor(dbActorSystem: ActorSystem<ParseFileActor>) {
    // TODO Task 3 - converts lines of csv file into actual data

    def parseStringToTick(path: String): Tick = {
        // TODO parse string into Tick object
        return new Tick(null, null, 0);
    }

    def apply(): Behavior[String] = {
        Behaviors.receive((context, message) => {
            message match {
                case "" =>
                    context.log.error("Not a valid data string...")
                    Behaviors.same
                case _ =>
                    context.log.info(
                      "Valid data string: '" + message + "'. Now parsing into data object..."
                    )
                    dbActorSystem ! parseStringToTick(message);
                    Behaviors.same
            }
        })
    }
}
