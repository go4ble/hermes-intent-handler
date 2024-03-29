package hermesIntentHandler.hermes

import hermesIntentHandler.hermes.IntentPayload._
import play.api.libs.json._

case class IntentPayload(
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
  def getSlot[T](slotName: String)(implicit reads: Reads[T]): Option[T] =
    slots.find(_.slotName == slotName).map(_.value.value.as[T])
}

object IntentPayload {
  case class HermesIntentRecognition(intentName: String, confidenceScore: BigDecimal)

  case class HermesIntentSlotValue(kind: String, value: JsValue)

  case class HermesIntentSlot(entity: String, slotName: String, confidence: BigDecimal, rawValue: String, value: HermesIntentSlotValue, range: Option[JsObject])

  case class HermesIntentAsrToken(value: String, confidence: BigDecimal, rangeStart: Int, rangeEnd: Int)

  implicit val hermesIntentAsrTokenReads: Reads[HermesIntentAsrToken] = Json.reads
  implicit val hermesIntentSlotValueReads: Reads[HermesIntentSlotValue] = Json.reads
  implicit val hermesIntentSlotReads: Reads[HermesIntentSlot] = Json.reads
  implicit val hermesIntentRecognitionReads: Reads[HermesIntentRecognition] = Json.reads
  implicit val hermesIntentReads: Reads[IntentPayload] = Json.reads

  def fromPayload(payload: Array[Byte]): IntentPayload = Json.parse(payload).as[IntentPayload]
}
