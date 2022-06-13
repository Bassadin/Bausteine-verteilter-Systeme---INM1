package BVS_gRPC

import Dependencies.{Tick, TickDatabase}
import de.hfu.protos.aufgabe_05_grpc.{AddTickRequest, AddTickResponse, DbAccessorGrpc, GetTickRequest, GetTickResponse}
import io.grpc.ServerBuilder

import java.util.logging.Logger
import scala.concurrent.{ExecutionContext, Future}

class DbAccessorServiceImplementation extends DbAccessorGrpc.DbAccessor {
    override def addTick(request: AddTickRequest): Future[AddTickResponse] = {
        val reply = TickDatabase.storeTickInDB(Tick(request.symbol, request.timestamp, request.price))
        Future.successful(AddTickResponse(reply))
    }

    override def getTick(request: GetTickRequest): Future[GetTickResponse] = {
        val reply = TickDatabase.getTickFromDB(request.symbol);
        Future.successful(GetTickResponse(reply.symbol, reply.timestampString, reply.price))
    }
}

object GRPC_Server extends App {
    val logger = Logger.getLogger(GRPC_Server.getClass.getName)

    val port = 50051
    val host = "localhost"

    val addTickService = DbAccessorGrpc.bindService(new DbAccessorServiceImplementation(), ExecutionContext.global)
    val server = ServerBuilder.forPort(port).addService(addTickService).asInstanceOf[ServerBuilder[_]].build().start()

    logger.info("server started, listening on port " + port)

    server.awaitTermination()
}
