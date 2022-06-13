package BVS_gRPC

import de.hfu.protos.hello._
import io.grpc.ServerBuilder

import java.util.logging.Logger
import scala.concurrent.{ExecutionContext, Future}

class GreeterImpl extends GreeterGrpc.Greeter {
    override def sayHello(request: HelloRequest): Future[HelloReply] = {
        val result = "Hello " + request.name
        val reply = HelloReply(result)
        return Future.successful(reply)
    }
}

object GRPC_Server extends App {
    val logger = Logger.getLogger(GRPC_Server.getClass.getName)

    val port = 50051
    val host = "localhost"

    val service = GreeterGrpc.bindService(new GreeterImpl(), ExecutionContext.global)
    val server = ServerBuilder.forPort(port).addService(service).asInstanceOf[ServerBuilder[_]].build().start()

    logger.info("server started, listening on port " + port)

    server.awaitTermination()
}
