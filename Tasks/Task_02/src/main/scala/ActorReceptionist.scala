import akka.NotUsed
import akka.actor.typed.Behavior
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.Behaviors

object ActorReceptionist {

    val guardian: Behavior[NotUsed] = Behaviors.setup { context =>
        val fileParserActor =
            context.spawn(ParseFileActor.apply(), "fileParserActor");
        context.system.receptionist ! Receptionist.register(
          ParseFileActor.serviceKey,
          fileParserActor
        );

        val dataConverterActor =
            context.spawn(ConvertDataActor.apply(), "dataConverterActor");
        context.system.receptionist ! Receptionist.register(
          ConvertDataActor.serviceKey,
          dataConverterActor
        );

        val databaseConnectorActor = context.spawn(
          DatabaseConnectorActor.apply(),
          "databaseConnectorActor"
        );
        context.system.receptionist ! Receptionist.register(
          DatabaseConnectorActor.serviceKey,
          databaseConnectorActor
        );

        Behaviors.empty;
    }

}
