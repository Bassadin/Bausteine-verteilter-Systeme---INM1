import akka.actor.typed.receptionist.Receptionist.{Find, Listing}
import akka.actor.typed.receptionist.ServiceKey
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import akka.util.Timeout

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import scala.util.Success

trait ConvertDataActorProtocol
object EndConvertDataActor extends ConvertDataActorProtocol
case class SendDataToConvertAndFindDBActor(newData: String)
    extends ConvertDataActorProtocol
case class SendFileDataToAveragerActor(
    dbActorRef: ActorRef[AveragerActorProtocol],
    newData: String
) extends ConvertDataActorProtocol

object ConvertDataActor {
    val serviceKey: ServiceKey[ConvertDataActorProtocol] =
        ServiceKey[ConvertDataActorProtocol]("convertDataActor")

    val formatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss.SSS")

    def parseStringToTick(inputDataString: String): Tick = {
        // Use -1 here so that the zero-length strings in the end also get thrown into the array
        val splitData: Array[String] = inputDataString.split(",", -1)

        // ID
        val newTickID: String = splitData(0)

        // Date and Time
        // https://www.java67.com/2016/04/how-to-convert-string-to-localdatetime-in-java8-example.html
        val dateString: String = splitData(26)
        if (dateString.isEmpty) {
            return null
        }

        val timeString: String = splitData(23)
        if (timeString.isEmpty || timeString == "00:00:00.000") {
            return null
        }

        val newTickDateTime: LocalDateTime =
            LocalDateTime.parse(dateString + " " + timeString, formatter)

        // Price
        // Double and long conversion to get correct values. Maybe improve later?
        val newTickPrice: Long = splitData(21).toDouble.toLong

        val newParsedTick = Tick(newTickID, newTickDateTime, newTickPrice)

        newParsedTick
    }

    def apply(): Behavior[ConvertDataActorProtocol] = {

        Behaviors.setup[ConvertDataActorProtocol] { context =>
            implicit val timeout: Timeout =
                Timeout.apply(100, TimeUnit.MILLISECONDS)

            Behaviors.receiveMessage {
                case SendDataToConvertAndFindDBActor(newData) =>
                    context.ask(
                      context.system.receptionist,
                      Find(AveragerActor.serviceKey)
                    ) { case Success(listing: Listing) =>
                        val instances =
                            listing.serviceInstances(AveragerActor.serviceKey)
                        val averagerActorRef =
                            instances.iterator.next()

                        SendFileDataToAveragerActor(averagerActorRef, newData);
                    }

                    Behaviors.same
                case SendFileDataToAveragerActor(averagerActorRef, newData) =>
                    val newTick: Tick = parseStringToTick(newData)

                    // Use NaN instead of null
                    if (newTick != null) {
                        averagerActorRef ! GetDBActorRefAndSendAveragerTickData(
                          newTick
                        )
                    }

                    Behaviors.same
                case EndConvertDataActor =>
                    context.log.info("Terminating convert data actor...")
                    Behaviors.stopped
            }
        }
    }

}
