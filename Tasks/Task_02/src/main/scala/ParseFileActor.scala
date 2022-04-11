import akka.NotUsed

import scala.io.Source
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.ActorSystem
import akka.actor.typed.receptionist.ServiceKey

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

    // def controller(): Behavior[NotUsed] = ???;

    def apply(): Behavior[ParseFileActorProtocol] = {
        Behaviors.receive { (context, message) =>
            {
                message match {
                    case StopParseFileActor =>
                        context.log.info("Terminating parse file actor...")
                        Behaviors.stopped;
                    case FileNameToParse(newData) =>
                        context.log.info(
                          "Valid file path: " + message + ". Now parsing..."
                        )
                        parseFileFrom(newData, context);
                        Behaviors.same;
                }
            }
        }
    }
}
