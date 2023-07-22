package hermesIntentHandler.hermes.dialogueManager

import hermesIntentHandler.MqttPayload
import hermesIntentHandler.hermes.dialogueManager.EndSession.endSessionPayloadWrites
import play.api.libs.json.{Json, OWrites}

/** Indicates a session has terminated
  * @param sessionId
  *   current session ID
  * @param text
  *   sentence to speak using text-to-speech
  * @param customData
  *   user-defined data (overrides session `customData` if not null)
  */
case class EndSession(sessionId: String, text: Option[String] = None, customData: Option[String] = None) extends MqttPayload {
  override val topic: String = "hermes/dialogueManager/endSession"
  override val payload: Array[Byte] = Json.toBytes(endSessionPayloadWrites.writes(this))
}

object EndSession {
  implicit val endSessionPayloadWrites: OWrites[EndSession] = Json.writes
}
