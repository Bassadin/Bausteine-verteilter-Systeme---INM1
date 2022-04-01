import java.time.LocalDateTime
final case class Tick(symbol: String, timestamp: LocalDateTime, price: Long)
