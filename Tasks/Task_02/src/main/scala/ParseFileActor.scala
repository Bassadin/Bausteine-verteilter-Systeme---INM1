import akka.NotUsed

import scala.io.Source
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.ActorSystem
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}

trait ParseFileActorProtocol
object StopParseFileActor extends ParseFileActorProtocol;
case class FileNameToParse(newData: String) extends ParseFileActorProtocol;

object ParseFileActor {
    val serviceKey = ServiceKey[ParseFileActorProtocol]("fileParser");

    val convertDataActor = ActorSystem(ConvertDataActor(), "hfu");

    def parseFileFrom(
        path: String,
        context: ActorContext[ParseFileActorProtocol]
    ): Unit = {
        // https://alvinalexander.com/scala/how-to-open-read-text-files-in-scala-cookbook-examples/
        // Drop first 4 lines since they're just headers
        for (line <- Source.fromFile(path).getLines.drop(4)) {
            convertDataActor ! DataToConvert(line);
        }

        // Quit the convert data actor afterwards
        convertDataActor ! EndConvertDataActor;
    }

    def apply(): Behavior[ParseFileActorProtocol] = {

        Behaviors.setup { context =>
            context.system.receptionist ! Receptionist.register(
              ParseFileActor.serviceKey,
              context.self
            )

            val listingResponseAdapter =
                context.messageAdapter[Receptionist.Listing](
                  ListingResponse.apply
                )

            context.system.receptionist ! Receptionist.Find(
              ConvertDataActor.serviceKey,
              listingResponseAdapter
            )

            Behaviors.receiveMessage {
                case StopParseFileActor =>
                    context.log.info("Terminating parse file actor...")
                    Behaviors.stopped;
                case FileNameToParse(newData) =>
                    context.log.info(
                      "Valid file path: " + newData + ". Now parsing..."
                    )
                    parseFileFrom(newData, context);
                    Behaviors.same;

            }
        }
    }
}
