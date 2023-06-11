package hermesIntentHandler.hermes.tts

import hermesIntentHandler.MqttPayload
import hermesIntentHandler.hermes.tts.SayFinishedPayload.sayFinishedPayloadWrites
import play.api.libs.json.{Json, OWrites}

/** Indicates that the text to speech system has finished speaking
  *
  * @param id
  *   unique ID for request (copied from SayPayload)
  * @param siteId
  *   Hermes site ID
  * @param sessionId
  *   current session ID
  */
case class SayFinishedPayload(
    id: Option[String],
    siteId: String,
    sessionId: Option[String]
) extends MqttPayload {
  override val topic: String = "hermes/tts/sayFinished"
  override val payload: Array[Byte] = Json.toBytes(sayFinishedPayloadWrites.writes(this))
}

object SayFinishedPayload {
  implicit val sayFinishedPayloadWrites: OWrites[SayFinishedPayload] = Json.writes

  def apply(sayPayload: SayPayload): SayFinishedPayload =
    SayFinishedPayload(sayPayload.id, sayPayload.siteId, sayPayload.sessionId)
}
