import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
class DatabaseConnectorActor {
    def connectToDB(): Unit = {
        // TODO connect to the the h2 db

    }

    def storeInDB(newTick: Tick): Boolean = {
        // TODO store the tick in the h2 db

        return true;
    }

    def apply(): Behavior[Tick] = {
        Behaviors.receive((context, message) => {
            storeInDB(message);
            Behaviors.same;
        })
    }
}
