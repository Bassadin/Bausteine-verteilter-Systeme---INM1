package BVS_gRPC

import Dependencies.Tick
import de.hfu.protos.aufgabe_05_grpc.{AddTickRequest, DbAccessorGrpc, GetTickRequest}
import de.hfu.protos.hello.{GreeterGrpc, HelloReply, HelloRequest}
import io.grpc.ManagedChannelBuilder

import java.util.logging.Logger
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

object GRPC_Client extends App {
    def addSync(tick: Tick): Boolean = {
        val request = AddTickRequest(tick.symbol, tick.timestampString, tick.price)
        DbAccessorGrpc.blockingStub(channel).addTick(request).success
    }

    def addAsync(tick: Tick): Future[Boolean] = {
        val request = AddTickRequest(tick.symbol, tick.timestampString, tick.price)
        val response = DbAccessorGrpc.stub(channel).addTick(request)

        // TODO: Das kann so nicht richtig sein
        response.flatMap(response => Future { response.success })
    }

    def getSync(symbol: String): Tick = {
        val request = GetTickRequest(symbol)
        val response = DbAccessorGrpc.blockingStub(channel).getTick(request)
        Tick(response.symbol, response.timestamp, response.price)
    }

    def getAsync(symbol: String): Future[Tick] = {
        val request = GetTickRequest(symbol)
        val response = DbAccessorGrpc.stub(channel).getTick(request)

        // TODO: Das kann so nicht richtig sein
        response.flatMap(response => Future { Tick(response.symbol, response.timestamp, response.price) })
    }

    val logger = Logger.getLogger(GRPC_Client.getClass.getName)

    val port = 50051
    val host = "localhost"

    val channel =
        ManagedChannelBuilder.forAddress(host, port).usePlaintext().asInstanceOf[ManagedChannelBuilder[_]].build()

    logger.info("trying to get a tick synchronously")

    logger.info(getSync("MC.FR").toString)

    logger.info("terminating...")
}
