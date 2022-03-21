import akka.actor.Actor;

class SimpleActor extends Actor {
    def receive(): PartialFunction[Any, Unit] = {
        // TOASK: case means that this only happens when such a mesage is actually passed to the function?
        case message => println("actor received: " + message);
    }
}
