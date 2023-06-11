package hermesIntentHandler.hermes.dialogueManager

import hermesIntentHandler.MqttPayload
import hermesIntentHandler.hermes.dialogueManager.EndSessionPayload.endSessionPayloadWrites
import play.api.libs.json.{Json, OWrites}

case class EndSessionPayload(sessionId: String, text: Option[String] = None) extends MqttPayload {
  override val topic: String = "hermes/dialogueManager/endSession"
  override val payload: Array[Byte] = Json.toBytes(endSessionPayloadWrites.writes(this))
}

object EndSessionPayload {
  implicit val endSessionPayloadWrites: OWrites[EndSessionPayload] = Json.writes
}
