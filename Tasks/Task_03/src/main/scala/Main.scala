import akka.actor.typed.{ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import com.typesafe.config.ConfigFactory

object Main extends App {
    // GitHub-Repo: https://github.com/Bassadin/Bausteine-verteilter-Systeme-INM1

    object RootBehavior {
        def apply(): Behavior[Nothing] = Behaviors.setup[Nothing] { context =>
            // Create an actor that handles cluster domain events
            context.spawn(ClusterListener(), "ClusterListener")

            Behaviors.empty
        }
    }

    def startup(port: Int): Unit = {
        val config = ConfigFactory
            .parseString(s"akka.remote.artery.canonical.port=$port")
            .withFallback(ConfigFactory.load())

        //        println("starting...")
        //
        //        val actorSystem: ActorSystem[ActorManager.ActorManagerProtocol] =
        //            ActorSystem[ActorManager.ActorManagerProtocol](
        //                ActorManager(),
        //                "hfu"
        //                , config
        //            )
        //        actorSystem ! ActorManager.SetupActorManager
        ////        actorSystem ! ActorManager.InitializeSystemWithFilePath("./test_ticks.csv")
        //
        //        println("terminating...")


        ActorSystem[Nothing](RootBehavior(), "hfu", config)
    }


    val ports =
        if (args.isEmpty)
            Seq(25251, 25252, 0)
        else
            args.toSeq.map(_.toInt)
    ports.foreach(startup)
}
