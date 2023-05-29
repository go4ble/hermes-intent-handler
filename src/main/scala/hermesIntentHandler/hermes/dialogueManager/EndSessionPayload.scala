package hermesIntentHandler.hermes.dialogueManager

import play.api.libs.json.{Json, OWrites}

case class EndSessionPayload(sessionId: String, text: Option[String] = None)

object EndSessionPayload {
  implicit val endSessionPayloadWrites: OWrites[EndSessionPayload] = Json.writes
}
