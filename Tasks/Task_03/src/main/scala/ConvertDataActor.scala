import ParseFileActor.AskForWork
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object ConvertDataActor {
    trait ConvertDataActorProtocol extends ActorProtocolSerializable

    case class HandleFileBatchedLines(newData: Seq[String], parserRef: ActorRef[ParseFileActor.ParseFileActorProtocol])
        extends ConvertDataActorProtocol
    case class WorkReady(parserRef: ActorRef[ParseFileActor.ParseFileActorProtocol]) extends ConvertDataActorProtocol
    case class ListingResponse(listing: Receptionist.Listing) extends ConvertDataActorProtocol
    case class Terminate() extends ConvertDataActorProtocol

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

        val newTickDateTime: LocalDateTime = LocalDateTime.parse(dateString + " " + timeString, formatter)

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
            context.log.info("--- Convert Data Actor UP ---")

            context.system.receptionist ! Receptionist.register(this.serviceKey, context.self)
            val subscriptionAdapter = context.messageAdapter[Receptionist.Listing](ListingResponse.apply)
            context.system.receptionist ! Receptionist.Subscribe(AveragerActor.serviceKey, subscriptionAdapter)

            Behaviors.receiveMessagePartial {
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
                case HandleFileBatchedLines(newData, parserRef) =>
                    context.self ! HandleFileBatchedLines(newData, parserRef)
                    Behaviors.same;
                case WorkReady(parserRef) =>
                    context.self ! WorkReady(parserRef)
                    Behaviors.same
            }
        }
    }

    private def handleAveragerRef(
        averagerActorRef: ActorRef[AveragerActor.AveragerActorProtocol]
    ): Behavior[ConvertDataActorProtocol] = Behaviors.setup { context =>
        Behaviors.receiveMessagePartial {
            case HandleFileBatchedLines(newDataLines: Seq[String], parserRef) =>
                newDataLines.foreach(eachLine => {
                    val newTick: Tick = parseStringToTick(eachLine)
                    if (newTick != null) {
                        averagerActorRef ! AveragerActor.HandleNewTickData(newTick)
                    }
                })
                parserRef ! AskForWork(context.self)
                Behaviors.same
            case WorkReady(parserRef) =>
                context.log.info("Work ready ask for work")
                parserRef ! AskForWork(context.self)
                Behaviors.same
            case this.Terminate() =>
                context.log.info("Terminating Convert Data Actor")
                averagerActorRef ! AveragerActor.Terminate()
                Behaviors.stopped
        }
    }
}
