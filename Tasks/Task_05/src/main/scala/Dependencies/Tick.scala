package Dependencies

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

final case class Tick(symbol: String, timestampString: String, price: Long) {
    override def toString: String = symbol + "; " + timestampString + "; " + price.toString
}

trait TickSprayJson extends SprayJsonSupport with DefaultJsonProtocol {
    implicit val tickFormat: RootJsonFormat[Tick] = jsonFormat3(Tick)
}
