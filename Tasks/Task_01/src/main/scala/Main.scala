import akka.actor.typed.ActorSystem

object Main extends App {
    println("starting...");
    val parseFileActor = ActorSystem(ParseFileActor(), "fileParser");

    // Send the file path to the parsing actor
    parseFileActor ! ParseFileData("test_ticks.csv");

    // End the actor afterwards
    parseFileActor ! StopParseFileActor;
}
