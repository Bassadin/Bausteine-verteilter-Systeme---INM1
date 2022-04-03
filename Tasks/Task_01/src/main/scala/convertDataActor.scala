import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.ActorSystem

import java.time.format.DateTimeFormatter
import java.time.LocalDateTime

trait ConvertDataActorProtocol
object EndConvertDataActor extends ConvertDataActorProtocol;
case class DataToConvert(newData: String) extends ConvertDataActorProtocol;

object ConvertDataActor {
    val dbConnectorActor = ActorSystem(DatabaseConnectorActor(), "hfu");

    def parseStringToTick(
        inputDataString: String,
        context: ActorContext[ConvertDataActorProtocol]
    ): Tick = {
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
        // Double and long conversion to get correct values. Maybe improve later?
        val newTickPrice: Long = splitData(21).toDouble.toLong;
        // Without the following part, the targeted 679 data points are put out. With it, the db is only 612 lines long for some reason.
//        if (newTickPrice == 0) {
//            return null;
//        }

        val newParsedTick = new Tick(newTickID, newTickDateTime, newTickPrice);

        return newParsedTick;
    }

    def apply(): Behavior[ConvertDataActorProtocol] = {
        Behaviors.receive { (context, message) =>
            message match {
                case DataToConvert(newData) =>
                    val newTick: Tick = parseStringToTick(newData, context);

                    if (newTick != null) {
                        dbConnectorActor ! TickData(newTick)
                    };
                    Behaviors.same
                case EndConvertDataActor =>
                    context.log.info("Terminating convert data actor...")
                    dbConnectorActor ! EndDbActor;
                    Behaviors.stopped
            }
        }
    }
}
