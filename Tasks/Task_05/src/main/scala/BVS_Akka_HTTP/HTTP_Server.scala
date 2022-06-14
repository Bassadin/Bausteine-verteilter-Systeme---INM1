package BVS_Akka_HTTP

import akka.actor.typed.ActorSystem
import akka.actor.typed.javadsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives._

import scala.io.StdIn

object HTTP_Server {
    def main(args: Array[String]) = {
        implicit val system = ActorSystem(Behaviors.empty, "http-server")
        implicit val executionContext = system.executionContext

        val route = {
            path("hello") {
                get {
                    complete { HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Say hello to akka-http!</h1>") }
                }
            }
        }

        val bindingFuture = Http().newServerAt("localhost", 8080).bind(route)

        println(s"Server now online. Pleas navigate to http://localhost:8080/hello\nPress RETURN to stop...")
        StdIn.readLine()
        bindingFuture.flatMap(_.unbind()).onComplete(_ => system.terminate())
    }
}
