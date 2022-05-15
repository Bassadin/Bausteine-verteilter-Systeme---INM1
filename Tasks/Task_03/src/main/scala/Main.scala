import ParseFileActor.SendFileDataToConvertActor
import Utils.createConfigWithPortAndRole
import akka.actor.typed.ActorSystem
import akka.cluster.typed.Cluster
import com.typesafe.config.{Config, ConfigFactory}

trait MySerializable;

object Main extends App {
    // GitHub-Repo: https://github.com/Bassadin/Bausteine-verteilter-Systeme-INM1

    // Create the actor systems with unique ports
    println("Creating Actor Systems")

//    ActorSystem(
//      ClusterEventsListener(),
//      "hfu",
//      createConfigWithPortAndRole(25251, "listener")
//    )

    val parseFileActorSystem = ActorSystem(
      ParseFileActor("./test_ticks.csv"),
      "hfu",
      createConfigWithPortAndRole(25252, "parse")
    )

    ActorSystem(
      ConvertDataActor(),
      "hfu",
      createConfigWithPortAndRole(25251, "convert")
    )
    ActorSystem(
      AveragerActor(),
      "hfu",
      createConfigWithPortAndRole(0, "averager")
    )
    ActorSystem(
      DatabaseConnectorActor(),
      "hfu",
      createConfigWithPortAndRole(0, "database")
    )

//    parseFileActorSystem ! SendFileDataToConvertActor("./test_ticks.csv");

    println("Finished creating Actor Systems")
}
