import akka.actor.typed.ActorSystem

object Main extends App {
    println("starting...")

    val actorSystem: ActorSystem[ActorManager.ActorManagerProtocol] =
        ActorSystem[ActorManager.ActorManagerProtocol](
          ActorManager(),
          "actorSystem"
        )
    actorSystem ! ActorManager.SetupActorManager
    actorSystem ! ActorManager.InitializeSystemWithFilePath("./test_ticks.csv")
//    actorSystem ! ActorManager.TerminateSystem

    println("terminating...")
}
