import akka.actor.typed.ActorSystem

object Main extends App {
    println("starting...");
    val parseFileActor = ActorSystem(ParseFileActor(), "hfu");

    // Send the file path to the parsing actor
    parseFileActor ! FileNameToParse("./test_ticks.csv");

    // End the actor afterwards
    parseFileActor ! StopParseFileActor;

    println("terminating...");
}
