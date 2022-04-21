import akka.actor.typed.ActorSystem

object Main extends App {
    println("starting...")

    val actorSystem: ActorSystem[ActorManagerProtocol] =
        ActorSystem[ActorManagerProtocol](ActorManager(), "actorSystem")
    actorSystem ! SetupActorManager
    actorSystem ! InitializeSystemWithFilePath("./test_ticks.csv")
//    actorSystem.terminate()

    println("terminating...")
}
