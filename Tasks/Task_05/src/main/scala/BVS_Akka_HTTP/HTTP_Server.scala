package BVS_Akka_HTTP

import Dependencies.{Tick, TickDatabase, TickSprayJson}
import akka.actor.typed.ActorSystem
import akka.actor.typed.javadsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.server.Directives._
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

import scala.io.StdIn

object HTTP_Server extends Directives with TickSprayJson {
    def main(args: Array[String]) = {
        implicit val system = ActorSystem(Behaviors.empty, "http-server")
        implicit val executionContext = system.executionContext

        val route = {
            path("getTick") {
                get {
                    parameters("tickSymbol") { (tickSymbol) =>
                        println(tickSymbol)
                        complete { TickDatabase.getTickFromDB(tickSymbol) }

                    }
                }
            }
        }

        val bindingFuture = Http().newServerAt("localhost", 8080).bind(route)

        println(s"Server now online.\nPress RETURN to stop...")
        StdIn.readLine()
        bindingFuture.flatMap(_.unbind()).onComplete(_ => system.terminate())
    }
}
