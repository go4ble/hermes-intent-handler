package hermesIntentHandler.hermes

import hermesIntentHandler.hermes.HermesIntent._
import play.api.libs.json._

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
}

object HermesIntent {
  case class HermesIntentRecognition(intentName: String, confidenceScore: BigDecimal)

  case class HermesIntentSlotValue(kind: String, value: JsValue)

  case class HermesIntentSlot(entity: String, slotName: String, confidence: BigDecimal, rawValue: String, value: HermesIntentSlotValue, range: Option[JsObject])

  case class HermesIntentAsrToken(value: String, confidence: BigDecimal, rangeStart: Int, rangeEnd: Int)

  implicit val hermesIntentAsrTokenReads: Reads[HermesIntentAsrToken] = Json.reads
  implicit val hermesIntentSlotValueReads: Reads[HermesIntentSlotValue] = Json.reads
  implicit val hermesIntentSlotReads: Reads[HermesIntentSlot] = Json.reads
  implicit val hermesIntentRecognitionReads: Reads[HermesIntentRecognition] = Json.reads
  implicit val hermesIntentReads: Reads[HermesIntent] = Json.reads
}
