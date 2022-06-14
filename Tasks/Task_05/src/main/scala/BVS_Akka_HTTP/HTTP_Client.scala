package BVS_Akka_HTTP

import Dependencies.Tick
import akka.actor.typed.ActorSystem
import akka.actor.typed.javadsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.unmarshalling.Unmarshal

import java.util.logging.Logger
import scala.concurrent.Future
import scala.util.{Failure, Success}

object HTTP_Client {

    implicit val system = ActorSystem(Behaviors.empty, "http-client-single-request")
    implicit val executionContext = system.executionContext

    def getAsync(symbol: String): Future[Tick] = ???

    val logger: Logger = Logger.getLogger(HTTP_Client.getClass.getName)

    def main(args: Array[String]) = {
        val responseFuture: Future[HttpResponse] =
            Http().singleRequest(HttpRequest(uri = "http://localhost:8080/getTick?tickSymbol=RUI.FR"))

        responseFuture.onComplete {
            case Success(response) =>
                println(response)
                Unmarshal(response.entity).to[String].onComplete { case Success(text) =>
                    println(text)
                }
            case Failure(_) => sys.error("something wrong")
        }
    }
}
