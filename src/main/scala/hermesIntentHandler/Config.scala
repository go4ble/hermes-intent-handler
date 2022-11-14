package hermesIntentHandler

import com.typesafe.config.ConfigFactory

import java.time.ZoneId
import scala.jdk.CollectionConverters._
import scala.util.Try

object Config {
  private val underlying = ConfigFactory.load()

  lazy val siteIds: Seq[String] = Try(underlying.getStringList("site-ids"))
    .map(_.asScala.toSeq)
    .getOrElse(Seq(underlying.getString("site-ids")))

  lazy val timeZone: ZoneId = Try(underlying.getString("time-zone")).map(ZoneId.of).getOrElse(ZoneId.systemDefault())

  object hass {
    lazy val host: String = underlying.getString("hass.host")
    lazy val token: String = underlying.getString("hass.token")
  }

  object mqtt {
    lazy val broker: String = underlying.getString("mqtt.broker")
  }
}
