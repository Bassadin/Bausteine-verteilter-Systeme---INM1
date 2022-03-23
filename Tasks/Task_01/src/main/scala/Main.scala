import akka.actor.typed.ActorSystem

object Main {
    def main(args: Array[String]): Unit = {
        println("starting...")
        val actor = ActorSystem(ToUpper(), "hfu");

        actor ! "hello akka";
        actor ! "";

        println("terminating...")
    }
}
