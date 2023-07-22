package hermesIntentHandler.hermes.tts

import hermesIntentHandler.MqttPayload
import hermesIntentHandler.hermes.tts.SayFinished.sayFinishedPayloadWrites
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
case class SayFinished(
    id: Option[String],
    siteId: String,
    sessionId: Option[String]
) extends MqttPayload {
  override val topic: String = "hermes/tts/sayFinished"
  override val payload: Array[Byte] = Json.toBytes(sayFinishedPayloadWrites.writes(this))
}

object SayFinished {
  implicit val sayFinishedPayloadWrites: OWrites[SayFinished] = Json.writes

  def apply(sayPayload: Say): SayFinished =
    SayFinished(sayPayload.id, sayPayload.siteId, sayPayload.sessionId)
}
