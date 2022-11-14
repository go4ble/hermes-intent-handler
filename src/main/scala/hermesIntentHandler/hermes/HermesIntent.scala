package hermesIntentHandler.hermes

import hermesIntentHandler.hermes.HermesIntent._
import play.api.libs.json.{JsObject, Json, Reads}

case class HermesIntent(
    input: String,
    intent: HermesIntentRecognition,
    siteId: String,
    slots: Seq[HermesIntentSlot],
    sessionId: String,
    customData: Option[String],
    asrTokens: Seq[Seq[HermesIntentAsrToken]],
    asrConfidence: Option[BigDecimal],
    wakeWordId: Option[String]
) {
  val intentName: String = intent.intentName

  def getSlotValue(slotName: String): String = (for {
    slot <- slots.find(_.slotName == slotName)
    value <- (slot.value \ "value").asOpt[String]
  } yield value).getOrElse(throw new IllegalArgumentException(s"unable to find slot value for $slotName"))
}

object HermesIntent {
  case class HermesIntentRecognition(intentName: String, confidenceScore: BigDecimal)

  case class HermesIntentSlot(entity: String, slotName: String, confidence: BigDecimal, rawValue: String, value: JsObject, range: Option[JsObject])

  case class HermesIntentAsrToken(value: String, confidence: BigDecimal, rangeStart: Int, rangeEnd: Int)

  implicit val hermesIntentAsrTokenReads: Reads[HermesIntentAsrToken] = Json.reads
  implicit val hermesIntentSlotReads: Reads[HermesIntentSlot] = Json.reads
  implicit val hermesIntentRecognitionReads: Reads[HermesIntentRecognition] = Json.reads
  implicit val hermesIntentReads: Reads[HermesIntent] = Json.reads
}
