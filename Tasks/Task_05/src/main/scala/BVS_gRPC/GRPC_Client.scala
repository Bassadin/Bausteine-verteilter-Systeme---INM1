package BVS_gRPC

import Dependencies.Tick
import de.hfu.protos.aufgabe_05_grpc.{AddTickRequest, DbAccessorGrpc, GetTickRequest}
import de.hfu.protos.hello.{GreeterGrpc, HelloReply, HelloRequest}
import io.grpc.{ManagedChannel, ManagedChannelBuilder}

import java.util.logging.Logger
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Random, Success, Try}

object GRPC_Client {
    def addSync(tick: Tick): Boolean = {
        val request = AddTickRequest(tick.symbol, tick.timestampString, tick.price)
        DbAccessorGrpc.blockingStub(channel).addTick(request).success
    }

    def addAsync(tick: Tick): Future[Boolean] = {
        val request = AddTickRequest(tick.symbol, tick.timestampString, tick.price)
        val response = DbAccessorGrpc.stub(channel).addTick(request)

        response.map(response => response.success)
    }

    def getSync(symbol: String): Tick = {
        val request = GetTickRequest(symbol)
        val response = DbAccessorGrpc.blockingStub(channel).getTick(request)
        Tick(response.symbol, response.timestamp, response.price)
    }

    def getAsync(symbol: String): Future[Tick] = {
        val request = GetTickRequest(symbol)
        val response = DbAccessorGrpc.stub(channel).getTick(request)

        response.map(response => Tick(response.symbol, response.timestamp, response.price))
    }

    val logger: Logger = Logger.getLogger(GRPC_Client.getClass.getName)

    val port = 50051
    val host = "localhost"

    val channel: ManagedChannel = ManagedChannelBuilder
        .forAddress(host, port)
        .usePlaintext()
        .asInstanceOf[ManagedChannelBuilder[_]]
        .build()

    def main(args: Array[String]) = {
        logger.info("trying to get a tick synchronously")
        logger.info(getSync("MC.FR").toString)

        logger.info("trying to add a tick synchronously")
        val tickToAdd: Tick = Tick("ABCDEFG_" + Random.nextString(5), "2022-06-13 09:00:01.149000", 9999)
        logger.info(addSync(tickToAdd).toString)

        logger.info("trying to get a tick asynchronously")
        getAsync("MC.FR").onComplete(resolvedFuture => {
            logger.info(resolvedFuture.toString)
        })

        logger.info("trying to add a tick asynchronously")
        val secondTickToAdd: Tick = Tick("ZYXWV_" + Random.nextString(5), "2022-06-10 09:00:01.149000", 1111)
        addAsync(secondTickToAdd).onComplete(resolvedFuture => {
            logger.info(resolvedFuture.toString)
        })

        Thread.sleep(5000)
        logger.info("terminating...")

    }
}
