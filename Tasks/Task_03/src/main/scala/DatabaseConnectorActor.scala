import akka.actor.typed.Behavior
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}

import java.sql.{Connection, DriverManager, PreparedStatement, Statement}

object DatabaseConnectorActor {
    trait DatabaseConnectorActorProtocol extends MySerializable

    object TerminateDatabaseConnectorActor
        extends DatabaseConnectorActorProtocol

    case class AveragerTickData(tick: Tick)
        extends DatabaseConnectorActorProtocol

    val serviceKey: ServiceKey[DatabaseConnectorActorProtocol] =
        ServiceKey[DatabaseConnectorActorProtocol]("databaseConnectorActor")

    // Connection data
    val connection: Connection = DriverManager.getConnection(
      "jdbc:h2:./src/main/resources/test;mode=MySQL",
      "sa",
      ""
    )

    val preparedTickInsertStatement: PreparedStatement =
        connection.prepareStatement(
          "INSERT INTO TICKS (SYMBOL, TICKDATETIME, PRICE) VALUES (?, ?, ?)"
        )

    val dbClearStatement: Statement = connection.createStatement()
    dbClearStatement.executeUpdate("DELETE FROM TICKS")

    def storeInDB(
        newTick: Tick,
        context: ActorContext[DatabaseConnectorActorProtocol]
    ): Unit = {
        val sqlStatementToExecute = preparedTickInsertStatement

        sqlStatementToExecute.setString(1, newTick.symbol)
        sqlStatementToExecute.setString(2, newTick.timestamp.toString)
        sqlStatementToExecute.setLong(3, newTick.price)
        sqlStatementToExecute.executeUpdate()

        context.log.info(s"Added Tick '$newTick' to DB successfully.")
    }

    def apply(): Behavior[DatabaseConnectorActorProtocol] = {

        Behaviors.setup { context =>
            Behaviors.receiveMessage {
                // Store new averager Tick data in the DB
                case AveragerTickData(newTickToStore) =>
                    storeInDB(newTickToStore, context)
                    Behaviors.same;
                case TerminateDatabaseConnectorActor =>
                    context.system.receptionist ! Receptionist.Deregister(
                      this.serviceKey,
                      context.self
                    )

                    context.log.info(
                      "End signal received, terminating DB actor and closing DB connection"
                    )

                    connection.close()
                    Behaviors.stopped;
            }
        }
    }
}
