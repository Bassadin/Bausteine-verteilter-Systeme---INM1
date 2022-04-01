import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.ActorSystem
import java.time.format.DateTimeFormatter
import java.time.LocalDateTime
object ConvertDataActor {
    val dbConnectorActor = ActorSystem(DatabaseConnectorActor(), "databaseConnector");

    // TODO Task 3 - converts lines of csv file into actual data

    def parseStringToTick(inputDataString: String): Tick = {
        // TODO parse string into Tick object

        // Use -1 here so that the zero-length strings in the end also get thrown into the array
        val splitData: Array[String] = inputDataString.split(",", -1);

        // ID
        val newTickID: String = splitData(0);

        // Date and Time
        // https://www.java67.com/2016/04/how-to-convert-string-to-localdatetime-in-java8-example.html
        val dateString: String = splitData(26);
        if (dateString.isEmpty) {
            return null;
        }
        val timeString: String = splitData(23);
        if (timeString.isEmpty || timeString == "00:00:00.000") {
            return null;
        }
        val formatter: DateTimeFormatter =
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss.SSS");
        val newTickDateTime: LocalDateTime =
            LocalDateTime.parse(dateString + " " + timeString, formatter);

        // Price
        val newTickPrice: Long = splitData(21).toDouble.toLong;
        if (newTickPrice == 0) {
            return null;
        }

        return new Tick(newTickID, newTickDateTime, newTickPrice);
    }

    def apply(): Behavior[String] = {
        Behaviors.receive((context, message) => {
            message match {
                case "" =>
                    context.log.error("Not a valid data string...")
                    Behaviors.same
                case _ =>
                    val newTick: Tick = parseStringToTick(message);

                    if (newTick != null) {
                        context.log.info(
                            "Valid data string: '" + message + "'. Now parsing into data object..."
                        )
                        dbConnectorActor ! newTick
                    };
                    Behaviors.same
            }
        })
    }
}
