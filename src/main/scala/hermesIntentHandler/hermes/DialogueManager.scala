package hermesIntentHandler.hermes

import akka.actor.typed.ActorRef
import hermesIntentHandler.clients.MqttClientBehavior
import play.api.libs.json.{Json, OWrites}

object DialogueManager {
  private val EndSessionTopic = "hermes/dialogueManager/endSession"

  def endSession()(implicit intent: HermesIntent, mqttClient: ActorRef[MqttClientBehavior.MqttClientMessage]): Unit =
    endSession(EndSessionPayload(intent.sessionId, None))

  def endSession(text: String)(implicit intent: HermesIntent, mqttClient: ActorRef[MqttClientBehavior.MqttClientMessage]): Unit =
    endSession(EndSessionPayload(intent.sessionId, Some(text)))

  private def endSession(endSessionPayload: EndSessionPayload)(implicit mqttClient: ActorRef[MqttClientBehavior.MqttClientMessage]): Unit =
    mqttClient ! MqttClientBehavior.Publish(EndSessionTopic, Json.toBytes(Json.toJson(endSessionPayload)), qos = 2)

  private case class EndSessionPayload(sessionId: String, text: Option[String])
  private implicit val endSessionPayloadWrites: OWrites[EndSessionPayload] = Json.writes
}
