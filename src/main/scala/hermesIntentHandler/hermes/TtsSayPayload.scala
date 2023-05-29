package hermesIntentHandler.hermes

import play.api.libs.json.{Json, Reads}

case class TtsSayPayload(
    text: String,
    lang: Option[String],
    id: Option[String],
    volume: Option[BigDecimal],
    siteId: String,
    sessionId: Option[String]
) {
  require(volume.forall(v => v >= 0 && v <= 1))
}

object TtsSayPayload {
  implicit val hermesTTSSayPayloadReads: Reads[TtsSayPayload] = Json.reads

  def fromPayload(payload: Array[Byte]): TtsSayPayload = Json.parse(payload).as[TtsSayPayload]
}
