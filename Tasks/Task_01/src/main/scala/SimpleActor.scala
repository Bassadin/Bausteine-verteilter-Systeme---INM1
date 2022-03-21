import akka.actor.Actor;

class SimpleActor {
    def receive(): PartialFunction[Any, Unit] = {
        // TOASK: case means that this only happens when such a mesage is actually passed to the function?
        case message => println("actor received: " + message);
    }
}
