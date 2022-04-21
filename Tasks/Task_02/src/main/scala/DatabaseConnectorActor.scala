import akka.actor.typed.Behavior
import akka.actor.typed.receptionist.ServiceKey
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}

import java.sql.{Connection, DriverManager, PreparedStatement}

trait DatabaseConnectorActorProtocol

object EndDbActor extends DatabaseConnectorActorProtocol

case class TickData(tick: Tick) extends DatabaseConnectorActorProtocol

object DatabaseConnectorActor {
    val serviceKey: ServiceKey[DatabaseConnectorActorProtocol] =
        ServiceKey[DatabaseConnectorActorProtocol]("databaseConnectorActor")

    val connection: Connection = DriverManager.getConnection(
      "jdbc:h2:./src/main/resources/test;mode=MySQL",
      "sa",
      ""
    )

    val preparedSqlStatement: PreparedStatement =
        connection.prepareStatement(
          "REPLACE INTO TICKS (SYMBOL, TICKDATETIME, PRICE) VALUES (?, ?, ?)"
        )

    def storeInDB(
        newTick: Tick,
        context: ActorContext[DatabaseConnectorActorProtocol]
    ): Unit = {
        val sqlStatement = preparedSqlStatement
        sqlStatement.setString(1, newTick.symbol)
        sqlStatement.setString(2, newTick.timestamp.toString)
        sqlStatement.setLong(3, newTick.price)
        sqlStatement.executeUpdate()

        context.log.info(s"Added Tick '$newTick' to DB successfully.")
    }

    def apply(): Behavior[DatabaseConnectorActorProtocol] = {

        Behaviors.setup { context =>
            Behaviors.receiveMessage {
                case TickData(newTickToStore) =>
                    storeInDB(newTickToStore, context)
                    Behaviors.same;
                case EndDbActor =>
                    context.log.info(
                      "End signal received, terminating DB actor and closing DB connection"
                    )
                    connection.close()
                    Behaviors.stopped;

            }
        }
    }

}
