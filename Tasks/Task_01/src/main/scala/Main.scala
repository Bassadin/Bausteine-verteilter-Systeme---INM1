import akka.actor.typed.ActorSystem

/*

Unanswered questions:
- How to pass actor references into other actors (wrapper objects?)
- How to connect to h2 db?

*/

object Main extends App {
    println("starting...");
    val parseFileActor = ActorSystem(ParseFileActor(), "fileParser");
    val dbConnectorActor = ActorSystem(DatabaseConnectorActor(), "databaseConnector");
    val convertDataActor = ActorSystem(ConvertDataActor(dbConnectorActor), "fileParser");

    parseFileActor ! "E:/Downloads/Aufgabenblatt+01/test_ticks.csv";

    println("terminating...");
}
