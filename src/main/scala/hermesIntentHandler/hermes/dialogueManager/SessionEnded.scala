package hermesIntentHandler.hermes.dialogueManager

import hermesIntentHandler.hermes.dialogueManager.SessionEnded.TerminationReason
import play.api.libs.json.{JsError, JsSuccess, Json, Reads}

/** Indicates a session has terminated
  * @param termination
  *   reason for termination
  * @param sessionId
  *   current session ID
  * @param siteId
  *   Hermes site ID
  * @param customData
  *   user-defined data (copied from `startSession`)
  */
case class SessionEnded(
    termination: TerminationReason,
    sessionId: String,
    siteId: String,
    customData: Option[String]
)

object SessionEnded {
  val Topic = "hermes/dialogueManager/sessionEnded"

  sealed abstract class TerminationReason(val reason: String)
  case object Nominal extends TerminationReason("nominal")
  case object AbortedByUser extends TerminationReason("abortedByUser")
  case object IntentNotRecognized extends TerminationReason("intentNotRecognized")
  case object Timeout extends TerminationReason("timeout")
  case object Error extends TerminationReason("error")
  val terminationReasons: Map[String, TerminationReason] =
    Seq(Nominal, AbortedByUser, IntentNotRecognized, Timeout, Error).map(reason => reason.reason -> reason).toMap

  implicit val terminationReasonReads: Reads[TerminationReason] = _.validate[String].flatMap { string =>
    terminationReasons.get(string) match {
      case Some(reason) => JsSuccess(reason)
      case _            => JsError(s"unknown termination reason: $string")
    }
  }

  implicit val sessionEndedReads: Reads[SessionEnded] = Json.reads

  def fromPayload(payload: Array[Byte]): SessionEnded = Json.parse(payload).as[SessionEnded]
}
