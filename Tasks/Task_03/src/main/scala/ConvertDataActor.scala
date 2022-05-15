import ParseFileActor.ParseFile
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object ConvertDataActor {
    trait ConvertDataActorProtocol extends ActorProtocolSerializable

    case class HandleFileLineString(
        newData: String
    ) extends ConvertDataActorProtocol

    case class ListingResponse(listing: Receptionist.Listing)
        extends ConvertDataActorProtocol

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

    def apply(
        averagerActorRef: ActorRef[AveragerActor.AveragerActorProtocol] = null
    ): Behavior[ConvertDataActorProtocol] = {

        Behaviors.setup { context =>
            context.system.receptionist ! Receptionist.register(
              this.serviceKey,
              context.self
            )
            context.log.info("--- Convert Data Actor UP ---")

            val subscriptionAdapter =
                context.messageAdapter[Receptionist.Listing](
                  ListingResponse.apply
                )

            context.system.receptionist ! Receptionist.Subscribe(
              AveragerActor.serviceKey,
              subscriptionAdapter
            )

//            context.system.receptionist ! Receptionist.Subscribe(
//              ParseFileActor.serviceKey,
//              subscriptionAdapter
//            )

            Behaviors.receiveMessage {
                case ListingResponse(
                      AveragerActor.serviceKey.Listing(listings)
                    ) =>
                    listings.headOption match {
                        case Some(averagerRef) =>
                            context.log.info(
                              "Using averager ref {}",
                              averagerRef
                            )
                            handleAveragerRef(averagerRef)
                        case None =>
                            Behaviors.same
                    }
                case HandleFileLineString(newData) =>
                    context.self ! HandleFileLineString(newData);
                    Behaviors.same;

//                case ListingResponse(
//                      ParseFileActor.serviceKey.Listing(listings)
//                    ) =>
////                    listings.foreach(parserActorRef => parserActorRef)
//                    Behaviors.same
            }
        }
    }

    private def handleAveragerRef(
        averagerActorRef: ActorRef[AveragerActor.AveragerActorProtocol]
    ): Behavior[ConvertDataActorProtocol] = Behaviors.setup { context =>
        Behaviors.receiveMessage { case HandleFileLineString(newData) =>
            val newTick: Tick = parseStringToTick(newData)

            // Use NaN instead of null
            if (newTick != null) {
                averagerActorRef ! AveragerActor.HandleNewTickData(newTick)
            }

            Behaviors.same
        }
    }
}
