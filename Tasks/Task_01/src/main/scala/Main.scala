import akka.actor.typed.ActorSystem
object Main extends App {
    // val fileParser: ParseFileActor = new ParseFileActor();
    // fileParser.parseFileFrom("E:/Downloads/Aufgabenblatt+01/test_ticks.csv");

    println("starting...")
    val actor = ActorSystem(ParseFileActor(), "fileParser");

    actor ! "E:/Downloads/Aufgabenblatt+01/test_ticks.csv";

    println("terminating...")
}
