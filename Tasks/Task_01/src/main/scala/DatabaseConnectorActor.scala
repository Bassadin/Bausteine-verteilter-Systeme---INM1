import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import java.sql.DriverManager
import java.sql.Connection

object DatabaseConnectorActor {
    val conn: Connection =
        DriverManager.getConnection("jdbc:h2:C:/Users/basti/Documents/Git-Repos/Bausteine-verteilter-Systeme-INM1/Tasks/Task_01/src/main/resources/test", "sa", "");

    def connectToDB(): Unit = {
        // TODO connect to the the h2 db

    }

    def storeInDB(newTick: Tick): Boolean = {
        // TODO store the tick in the h2 db

        println("Printing Tick: " + newTick);

        return true;
    }

    def apply(): Behavior[Tick] = {
        Behaviors.receive((context, message) => {
            message match {
                case Tick(_, _, _) =>
                    context.log.info(
                        "Valid tick data..."
                    )
                    storeInDB(message);
                    Behaviors.same;
                case null =>
                    context.log.info(
                        "Null received, closing db connection"
                    )
                    conn.close();
                    Behaviors.stopped;
            }
        })

    }
}
