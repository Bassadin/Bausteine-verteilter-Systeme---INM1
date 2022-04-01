import akka.actor.typed.ActorSystem

object Main extends App {
    println("starting...");
    val parseFileActor = ActorSystem(ParseFileActor(), "fileParser");

    parseFileActor ! ParseFileData("test_ticks.csv");
    parseFileActor ! StopParseFileActor;

    println("terminating...");
}