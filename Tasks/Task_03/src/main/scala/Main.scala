import akka.actor.typed.ActorSystem

object Main extends App {
    // GitHub-Repo: https://github.com/Bassadin/Bausteine-verteilter-Systeme-INM1
    println("starting...")

    val actorSystem: ActorSystem[ActorManager.ActorManagerProtocol] =
        ActorSystem[ActorManager.ActorManagerProtocol](
          ActorManager(),
          "actorSystem"
        )
    actorSystem ! ActorManager.SetupActorManager
    actorSystem ! ActorManager.InitializeSystemWithFilePath("./test_ticks.csv")

    println("terminating...")
}
