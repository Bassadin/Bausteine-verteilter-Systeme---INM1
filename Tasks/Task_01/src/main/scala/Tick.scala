import java.time.LocalDateTime

final case class Tick(symbol: String, timestamp: LocalDateTime, price: Long) {
    override def toString: String = symbol + "; " + timestamp.toString + "; " + price.toString;
}
