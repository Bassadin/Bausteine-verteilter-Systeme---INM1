import akka.actor.typed.{ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import com.typesafe.config.{Config, ConfigFactory}

import java.lang.Thread
import scala.collection.immutable.HashMap

object Main extends App {
    // GitHub-Repo: https://github.com/Bassadin/Bausteine-verteilter-Systeme-INM1

    def createConfigWithPort(port: Int): Config = {
        ConfigFactory
            .parseString(s"akka.remote.artery.canonical.port=$port")
            .withFallback(ConfigFactory.load())
    }
    val actorManagerRef = ActorSystem(
        ActorManager(),
        "hfu",
        createConfigWithPort(25251)
    )

    println("Creating Actor Systems")

    ActorSystem(
        ParseFileActor(),
        "hfu",
        createConfigWithPort(25252)
    )
    ActorSystem(
        ConvertDataActor(),
        "hfu",
        createConfigWithPort(25551)
    )
    ActorSystem(
        AveragerActor(),
        "hfu",
        createConfigWithPort(25552)
    )
    ActorSystem(
        DatabaseConnectorActor(),
        "hfu",
        createConfigWithPort(25553)
    )

    println("Finished creating Actor Systems")

    actorManagerRef ! ActorManager.InitializeSystemWithFilePath(
        "./test_ticks.csv"
    )


}
