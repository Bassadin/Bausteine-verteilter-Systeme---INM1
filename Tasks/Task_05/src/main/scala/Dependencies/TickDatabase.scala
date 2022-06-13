package Dependencies

import java.sql.{Connection, DriverManager, PreparedStatement, ResultSet, Statement}

object TickDatabase {
    // Connection data
    val connection: Connection = DriverManager.getConnection("jdbc:h2:./src/main/resources/test;mode=MySQL", "sa", "")

    val preparedTickInsertStatement: PreparedStatement =
        connection.prepareStatement(
          "INSERT INTO TICKS (SYMBOL, TICKDATETIME, PRICE) VALUES (?, ?, ?)"
        )

    val preparedGetTickStatement: PreparedStatement =
        connection.prepareStatement(
          "SELECT * FROM TICKS WHERE SYMBOL = ?"
        )

    def storeTickInDB(
        newTick: Tick
    ): Boolean = {
        val sqlStatementToExecute = preparedTickInsertStatement

        try {
            sqlStatementToExecute.setString(1, newTick.symbol)
            sqlStatementToExecute.setString(2, newTick.timestampString)
            sqlStatementToExecute.setLong(3, newTick.price)
            sqlStatementToExecute.executeUpdate()
        } catch {
            case e: Exception =>
                e.printStackTrace()
                return false;
        }

        print(s"Added Tick '$newTick' to DB successfully.")
        return true;
    }

    def getTickFromDB(symbol: String): Tick = {
        val sqlStatementToExecute = preparedGetTickStatement

        sqlStatementToExecute.setString(1, symbol)
        val result: ResultSet = sqlStatementToExecute.executeQuery()

        result.next();

        Tick(
          result.getString("SYMBOL"),
          result.getTimestamp("TICKDATETIME").toString,
          result.getLong("PRICE")
        );
    }
}
