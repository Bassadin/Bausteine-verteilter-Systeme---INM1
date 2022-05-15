import Utils.createConfigWithPortAndRole
import akka.actor.typed.ActorSystem
trait ActorProtocolSerializable

object Main extends App {
    // GitHub-Repo: https://github.com/Bassadin/Bausteine-verteilter-Systeme-INM1

    println("Creating Actor Systems")

    ActorSystem(ParseFileActor("./test_ticks.csv"), "hfu", createConfigWithPortAndRole(25252, "parse"))
    ActorSystem(ConvertDataActor(), "hfu", createConfigWithPortAndRole(25251, "convert"))
    ActorSystem(AveragerActor(), "hfu", createConfigWithPortAndRole(0, "averager"))
    ActorSystem(DatabaseConnectorActor(), "hfu", createConfigWithPortAndRole(0, "database"))

    println("Finished creating Actor Systems")
}
