import scala.io.Source
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.ActorSystem

object ParseFileActor {
    val convertDataActor = ActorSystem(ConvertDataActor(), "fileParser");

    def parseFileFrom(path: String): Unit = {
        // https://alvinalexander.com/scala/how-to-open-read-text-files-in-scala-cookbook-examples/
        for (line <- Source.fromFile(path).getLines) {
            println(line);
        }
    }

    def apply(): Behavior[String] = {
        Behaviors.receive((context, message) => {
            message match {
                case "" =>
                    context.log.error("Not a valid file path...")
                    context.log.info("Terminating actor...")
                    Behaviors.stopped;
                case _ =>
                    context.log.info(
                      "Valid file path: " + message + ". Now parsing..."
                    )
                    parseFileFrom(message);
                    Behaviors.same;
            }
        })
    }
}
