import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}

import java.sql.DriverManager
import java.sql.Connection

trait DatabaseConnectorActorProtocol;

object EndDbActor extends DatabaseConnectorActorProtocol;

case class TickData(tick: Tick) extends DatabaseConnectorActorProtocol;

object DatabaseConnectorActor {
    val connection: Connection =
        DriverManager.getConnection("jdbc:h2:./src/main/resources/test", "sa", "");

    def storeInDB(newTick: Tick, context: ActorContext[DatabaseConnectorActorProtocol]): Unit = {
        try {
            val sqlStatementString: String = s"INSERT INTO TICKS values('${newTick.symbol}', '${newTick.timestamp}', ${newTick.price})"

            val sqlStatement = connection.prepareStatement(sqlStatementString);
            sqlStatement.executeUpdate();
            sqlStatement.close();
        } catch {
            case e: java.sql.SQLTimeoutException => context.log.error("Database timeout - " + e.toString)
            case e: java.sql.SQLException => context.log.error("SQL Exception - " + e.toString)
        }
        
        println(s"Added Tick $newTick to db successfully.");
    }

    def apply(): Behavior[DatabaseConnectorActorProtocol] = {
        Behaviors.receive((context, message) => {
            message match {
                case TickData(newTickToStore) =>
                    context.log.info(
                        "Valid tick data..."
                    )
                    storeInDB(newTickToStore, context);
                    Behaviors.same;
                case EndDbActor =>
                    context.log.info(
                        "Null received, closing db connection"
                    )
                    connection.close();
                    Behaviors.stopped;
            }
        })

    }
}
