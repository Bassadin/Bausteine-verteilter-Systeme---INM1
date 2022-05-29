import Utils.createConfigWithPortAndRole
import akka.actor.typed.ActorSystem
trait ActorProtocolSerializable

object Main extends App {
    // GitHub-Repo: https://github.com/Bassadin/Bausteine-verteilter-Systeme-INM1

    println("Creating Actor Systems")

    ActorSystem(ParseFileActor("./test_ticks.csv"), "hfu", createConfigWithPortAndRole(25252, "parse"))
    ActorSystem(ConvertDataActor(), "hfu", createConfigWithPortAndRole(25251, "convert"))

    ActorSystem(DatabaseConnectorActor(), "hfu", createConfigWithPortAndRole(0, "database"))

    ActorSystem(AveragerRouter(), "hfu", createConfigWithPortAndRole(0, "averagerRouter"))

    ActorSystem(AveragerActor(), "hfu", createConfigWithPortAndRole(0, "averagerActor01"))
    ActorSystem(AveragerActor(), "hfu", createConfigWithPortAndRole(0, "averagerActor02"))
    ActorSystem(AveragerActor(), "hfu", createConfigWithPortAndRole(0, "averagerActor03"))
    ActorSystem(AveragerActor(), "hfu", createConfigWithPortAndRole(0, "averagerActor04"))
    ActorSystem(AveragerActor(), "hfu", createConfigWithPortAndRole(0, "averagerActor05"))
    ActorSystem(AveragerActor(), "hfu", createConfigWithPortAndRole(0, "averagerActor06"))
    ActorSystem(AveragerActor(), "hfu", createConfigWithPortAndRole(0, "averagerActor07"))
    ActorSystem(AveragerActor(), "hfu", createConfigWithPortAndRole(0, "averagerActor08"))
    ActorSystem(AveragerActor(), "hfu", createConfigWithPortAndRole(0, "averagerActor09"))
    ActorSystem(AveragerActor(), "hfu", createConfigWithPortAndRole(0, "averagerActor10"))


    println("Finished creating Actor Systems")
}
