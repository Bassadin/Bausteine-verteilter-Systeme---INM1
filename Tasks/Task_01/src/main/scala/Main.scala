import akka.actor.typed.ActorSystem

/*

Unanswered questions:
- How to pass actor references into other actors (wrapper objects?)
- How to connect to h2 db?
- data in isngle string instances or as array?

*/

object Main extends App {
    println("starting...");
    val parseFileActor = ActorSystem(ParseFileActor(), "fileParser");

    parseFileActor ! "E:/Downloads/Aufgabenblatt+01/test_ticks.csv";

    println("terminating...");
}
