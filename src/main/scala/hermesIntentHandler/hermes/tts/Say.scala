package hermesIntentHandler.hermes.tts

import org.eclipse.paho.client.mqttv3.MqttMessage
import play.api.libs.json.{Json, Reads}

/** Generate spoken audio for a sentence using the configured text to speech system
  *
  * Automatically sends playBytes
  * @param text
  *   sentence to speak
  * @param lang
  *   override language for TTS system
  * @param id
  *   unique ID for request (copied to SayFinishedPayload)
  * @param volume
  *   volume level to speak with (0 = off, 1 = full volume)
  * @param siteId
  *   Hermes site ID
  * @param sessionId
  *   current session ID
  */
case class Say(
    text: String,
    lang: Option[String],
    id: Option[String],
    volume: Option[BigDecimal],
    siteId: String,
    sessionId: Option[String]
) {
  require(volume.forall(v => v >= 0 && v <= 1))
}

object Say {
  implicit val sayPayloadReads: Reads[Say] = Json.reads

  def fromPayload(payload: Array[Byte]): Say = Json.parse(payload).as[Say]
}
