import akka.actor.typed.ActorSystem
import com.typesafe.config.ConfigFactory

object Main extends App {
    // GitHub-Repo: https://github.com/Bassadin/Bausteine-verteilter-Systeme-INM1
    def startup(port: Int): Unit = {
        val config = ConfigFactory
            .parseString(s"akka.remote.artery.canonical.port=$port")
            .withFallback(ConfigFactory.load())

        println("starting...")

        val actorSystem: ActorSystem[ActorManager.ActorManagerProtocol] =
            ActorSystem[ActorManager.ActorManagerProtocol](
                ActorManager(),
                "hfu"
                , config
            )
        actorSystem ! ActorManager.SetupActorManager
        actorSystem ! ActorManager.InitializeSystemWithFilePath("./test_ticks.csv")

        println("terminating...")
    }

    startup(args(0).toInt)
}
