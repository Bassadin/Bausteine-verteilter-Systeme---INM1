package BVS_gRPC

import Dependencies.Tick
import de.hfu.protos.hello.{GreeterGrpc, HelloReply, HelloRequest}
import io.grpc.ManagedChannelBuilder

import java.util.logging.Logger
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

object GRPC_Client extends App {
    def addSync(tick: Tick): Boolean = ???
    def addAsync(tick: Tick): Future[Boolean] = ???
    def getSync(symbol: String): Tick = ???
    def getAsync(symbol: String): Future[Tick] = ???

    val logger = Logger.getLogger(GRPC_Client.getClass.getName)

    val port = 50051
    val host = "localhost"

    val channel =
        ManagedChannelBuilder.forAddress(host, port).usePlaintext().asInstanceOf[ManagedChannelBuilder[_]].build()

    val request = HelloRequest("gRPC")

    logger.info("try to greet " + request.name + " synchronously...")

    def logResponseCallback(response: Try[HelloReply]) {
        response match {
            case Success(greeting)  => logger.info("Greeting: " + greeting.message)
            case Failure(exception) => exception.printStackTrace()
        }
    }

    GreeterGrpc.stub(channel).sayHello(request).onComplete(logResponseCallback)

    Thread.sleep(5000)
}
