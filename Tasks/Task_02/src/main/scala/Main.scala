import akka.actor.typed.ActorSystem

object Main extends App {
    println("starting...");
    val system = ActorSystem[Nothing](Guardian(), "ActorDiscovery");
    println("terminating...");
}
