package BVS_gRPC

import Dependencies.Tick

import scala.concurrent.Future

class GRPC_Client {
    def addSync(tick: Tick): Boolean = ???
    def addAsync(tick: Tick): Future[Boolean] = ???
    def getSync(symbol: String): Tick = ???
    def getAsync(symbol: String): Future[Tick] = ???
}
