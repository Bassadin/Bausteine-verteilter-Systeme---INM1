import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
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

    println("Creating Actor Systems")

    val parseFileActorRef = ActorSystem(
        ParseFileActor(),
        "hfu",
        createConfigWithPort(25251)
    )
    ActorSystem(
        ConvertDataActor(),
        "hfu",
        createConfigWithPort(25252)
    )
    ActorSystem(
        AveragerActor(),
        "hfu",
        createConfigWithPort(47112)
    )
    ActorSystem(
        DatabaseConnectorActor(),
        "hfu",
        createConfigWithPort(47111)
    )

    println("Finished creating Actor Systems")

    parseFileActorRef ! ParseFileActor.LoadDataFromFileAndGetParseActor(
        "./test_ticks.csv"
    )


}
