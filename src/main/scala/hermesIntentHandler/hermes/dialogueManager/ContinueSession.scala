package hermesIntentHandler.hermes.dialogueManager

import hermesIntentHandler.MqttPayload
import hermesIntentHandler.hermes.dialogueManager.ContinueSession.continueSessionPayloadWrites
import play.api.libs.json.{Json, OWrites}

/** Requests that a session be continued after an `intent` has been recognized
  * @param sessionId
  *   current session ID
  * @param customData
  *   user-defined data (overrides session `customData` if not null)
  * @param text
  *   sentence to speak using text-to-speech
  * @param intentFilter
  *   valid intent names (null means all)
  * @param sendIntentNotRecognized
  *   send `hermes/dialogueManager/intentNotRecognized` if intent recognition fails
  */
case class ContinueSession(
    sessionId: String,
    customData: Option[String] = None,
    text: Option[String] = None,
    intentFilter: Option[Seq[String]] = None,
    sendIntentNotRecognized: Boolean = false
) extends MqttPayload {
  override val topic: String = "hermes/dialogueManager/continueSession"
  override val payload: Array[Byte] = Json.toBytes(continueSessionPayloadWrites.writes(this))
}

object ContinueSession {
  implicit val continueSessionPayloadWrites: OWrites[ContinueSession] = Json.writes
}
