package Dependencies

final case class Tick(symbol: String, timestampString: String, price: Long) {
    override def toString: String = symbol + "; " + timestampString + "; " + price.toString
}
