package hermesIntentHandler.hermes.asr

import hermesIntentHandler.MqttPayload
import hermesIntentHandler.hermes.asr.StartListening.startListeningWrites
import play.api.libs.json.{Json, OWrites}

/** Tell ASR system to start recording/transcribing
  * @param siteId
  *   Hermes site ID
  * @param sessionId
  *   current session ID
  * @param stopOnSilence
  *   detect silence and automatically end voice command (Rhasspy only)
  * @param sendAudioCaptured
  *   send `audioCaptured` after stop listening (Rhasspy only)
  * @param wakewordId
  *   ID of wake word that triggered session (Rhasspy only)
  * @param intentFilter
  *   undocumented
  */
case class StartListening(
    siteId: String,
    sessionId: Option[String] = None,
    stopOnSilence: Boolean = true,
    sendAudioCaptured: Boolean = false,
    wakewordId: Option[String] = None,
    intentFilter: Option[Seq[String]] = None
) extends MqttPayload {
  override val topic: String = "hermes/asr/startListening"
  override val payload: Array[Byte] = Json.toBytes(startListeningWrites.writes(this))
}

object StartListening {
  implicit val startListeningWrites: OWrites[StartListening] = Json.writes
}
