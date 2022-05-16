import akka.actor.typed.Behavior
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}

import java.sql.{Connection, DriverManager, PreparedStatement, Statement}

object DatabaseConnectorActor {
    trait DatabaseConnectorActorProtocol extends ActorProtocolSerializable

    case class HandleAveragedTickData(tick: Tick) extends DatabaseConnectorActorProtocol
    case class ListingResponse(listing: Receptionist.Listing) extends DatabaseConnectorActorProtocol
    case class Terminate() extends DatabaseConnectorActorProtocol

    val serviceKey = ServiceKey[DatabaseConnectorActorProtocol]("databaseConnectorActor")

    // Connection data
    val connection: Connection = DriverManager.getConnection("jdbc:h2:./src/main/resources/test;mode=MySQL", "sa", "")

    val preparedTickInsertStatement: PreparedStatement =
        connection.prepareStatement(
          "INSERT INTO TICKS (SYMBOL, TICKDATETIME, PRICE) VALUES (?, ?, ?)"
        )

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
            context.log.info("--- DELETING DATABASE ---")
            val dbClearStatement: Statement = connection.createStatement()
            dbClearStatement.executeUpdate("DELETE FROM TICKS")

            context.system.receptionist ! Receptionist.register(this.serviceKey, context.self)
            val subscriptionAdapter = context.messageAdapter[Receptionist.Listing](ListingResponse.apply)
            context.system.receptionist ! Receptionist.Subscribe(AveragerActor.serviceKey, subscriptionAdapter)

            Behaviors.receiveMessage {
                // Store new averager Tick data in the DB
                case HandleAveragedTickData(newTickToStore) =>
                    storeInDB(newTickToStore, context)
                    Behaviors.same;
                case ListingResponse(AveragerActor.serviceKey.Listing(listings)) =>
                    listings.foreach(averagerRef => averagerRef)
                    Behaviors.same
                case this.Terminate() =>
                    context.log.info("Terminating DB Actor")
                    connection.close()
                    Behaviors.stopped
            }
        }
    }
}
