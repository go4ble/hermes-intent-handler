package hermesIntentHandler.hermes.dialogueManager

import play.api.libs.json.{Json, Reads}

/** Sent when intent recognition fails during a session (only when `sendIntentNotRecognized = true`)
  * @param sessionId
  *   current session ID
  * @param siteId
  *   Hermes site ID
  * @param input
  *   input to NLU system
  * @param customData
  *   user-defined data
  */
case class IntentNotRecognized(sessionId: String, siteId: String, input: Option[String], customData: Option[String])

object IntentNotRecognized {
  implicit val intentNotRecognizedReads: Reads[IntentNotRecognized] = Json.reads
}
