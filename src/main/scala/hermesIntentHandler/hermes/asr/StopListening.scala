package hermesIntentHandler.hermes.asr

import hermesIntentHandler.MqttPayload
import hermesIntentHandler.hermes.asr.StopListening.stopListeningWrites
import play.api.libs.json.{Json, OWrites}

/** Tell ASR system to stop recording
  * @param siteId
  *   Hermes site ID
  * @param sessionId
  *   current session ID
  */
case class StopListening(siteId: String, sessionId: String) extends MqttPayload {
  override val topic: String = "hermes/asr/stopListening"
  override val payload: Array[Byte] = Json.toBytes(stopListeningWrites.writes(this))
}

object StopListening {
  implicit val stopListeningWrites: OWrites[StopListening] = Json.writes
}
