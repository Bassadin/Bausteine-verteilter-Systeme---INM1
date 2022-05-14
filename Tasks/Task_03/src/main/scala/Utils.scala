import com.typesafe.config.{Config, ConfigFactory}

object Utils {
    def createConfigWithPort(port: Int, role: String): Config = {
        ConfigFactory
            .parseString(s"""
                akka.remote.artery.canonical.port=$port
                akka.cluster.roles=[$role]
                """)
            .withFallback(ConfigFactory.load())
    }
}
