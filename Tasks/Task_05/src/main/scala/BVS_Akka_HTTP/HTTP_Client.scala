package BVS_Akka_HTTP

import Dependencies.{Tick, TickSprayJson}
import akka.actor.typed.ActorSystem
import akka.actor.typed.javadsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.unmarshalling.Unmarshal

import java.util.logging.Logger
import scala.concurrent.Future
import scala.util.{Failure, Success}

object HTTP_Client extends Directives with TickSprayJson {

    implicit val system = ActorSystem(Behaviors.empty, "http-client-single-request")
    implicit val executionContext = system.executionContext

    def getAsync(symbol: String): Future[Tick] = {
        val responseFuture: Future[HttpResponse] =
            Http().singleRequest(HttpRequest(uri = s"http://localhost:8080/getTick?tickSymbol=$symbol"))

        responseFuture.flatMap(response => Unmarshal(response.entity).to[Tick])
    }

    def main(args: Array[String]) = {
        println("testing getAsync function")
        getAsync("RUI.FR").onComplete {
            case Failure(exception)  => exception.printStackTrace()
            case Success(resultTick) => println(resultTick)
        }
    }
}
