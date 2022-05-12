import akka.actor.typed.ActorSystem
import akka.cluster.typed.Cluster
import com.typesafe.config.{Config, ConfigFactory}

trait MySerializable;

object Main extends App {
    // GitHub-Repo: https://github.com/Bassadin/Bausteine-verteilter-Systeme-INM1

    def createConfigWithPort(port: Int, role: String): Config = {
        ConfigFactory
            .parseString(s"""
                akka.remote.artery.canonical.port=$port
                akka.cluster.roles=[$role]
                """)
            .withFallback(ConfigFactory.load())
    }

    println("Creating Actor Systems")

    ActorSystem(
      ClusterListener(),
      "hfu",
      createConfigWithPort(25251, "listener")
    )

    val parseFileActorSystem = ActorSystem(
      ParseFileActor(),
      "hfu",
      createConfigWithPort(25252, "parse")
    )

    ActorSystem(
      ConvertDataActor(),
      "hfu",
      createConfigWithPort(47113, "convert")
    )
    ActorSystem(
      AveragerActor(),
      "hfu",
      createConfigWithPort(47112, "averager")
    )
    ActorSystem(
      DatabaseConnectorActor(),
      "hfu",
      createConfigWithPort(47111, "database")
    )

    println("Finished creating Actor Systems")

//    parseFileActorRef ! ParseFileActor.LoadDataFromFileAndGetParseActor(
//        "./test_ticks.csv"
//    )

}
