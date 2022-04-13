import akka.NotUsed
import akka.actor.typed.Behavior
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.Behaviors

object Guardian {

    def apply(): Behavior[Nothing] = {
        Behaviors
            .setup[Receptionist.Listing] { context =>
                val fileParserActor =
                    context.spawn(ParseFileActor.apply(), "fileParserActor");
                context.system.receptionist ! Receptionist.register(
                  ParseFileActor.serviceKey,
                  fileParserActor
                );

                val dataConverterActor =
                    context.spawn(
                      ConvertDataActor.apply(fileParserActor),
                      "dataConverterActor"
                    );
                context.system.receptionist ! Receptionist.register(
                  ConvertDataActor.serviceKey,
                  dataConverterActor
                );

                val databaseConnectorActor = context.spawn(
                  DatabaseConnectorActor.apply(dataConverterActor),
                  "databaseConnectorActor"
                );
                context.system.receptionist ! Receptionist.register(
                  DatabaseConnectorActor.serviceKey,
                  databaseConnectorActor
                );

                fileParserActor ! FileNameToParse("./test_ticks.csv");

                Behaviors.empty;
            }
            .narrow
    }

}
