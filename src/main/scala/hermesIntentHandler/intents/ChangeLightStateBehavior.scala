package hermesIntentHandler.intents

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import hermesIntentHandler.clients.{HomeAssistantClientBehavior, MqttClientBehavior}
import hermesIntentHandler.hermes.{IntentPayload, audioServer, dialogueManager}
import org.eclipse.paho.client.mqttv3.MqttMessage
import play.api.libs.json.Json

object ChangeLightStateBehavior {
  private val ChangeLightStateIntentTopic = "hermes/intent/ChangeLightState".r

  private val SlotNameLightName = "light_entity_id"
  private val SlotNameState = "state"
  private val SlotNameLightBrightnessPercentage = "light_brightness_percentage"

  sealed trait ChangeLightStateMessage
  private final case class HandleIntent(intent: IntentPayload) extends ChangeLightStateMessage
  private object HandleIntent {
    def apply(mqttMessage: MqttMessage): HandleIntent = HandleIntent(IntentPayload.fromPayload(mqttMessage.getPayload))
  }
  private final case class ChangeLightStateResponse(sessionId: String, siteId: String, response: Seq[HomeAssistantClientBehavior.StateResponse])
      extends ChangeLightStateMessage

  def apply(mqttClient: MqttClientBehavior.Actor, hassClient: HomeAssistantClientBehavior.Actor): Behavior[ChangeLightStateMessage] =
    Behaviors.setup { context =>
      mqttClient ! MqttClientBehavior.Subscribe(ChangeLightStateIntentTopic, context.messageAdapter { case (_, msg) => HandleIntent(msg) })

      Behaviors.receiveMessage {
        case HandleIntent(intent) =>
          val entityId = intent.getSlot[String](SlotNameLightName).get
          val state = intent.getSlot[String](SlotNameState).get
          val serviceData = intent.getSlot[BigDecimal](SlotNameLightBrightnessPercentage).map { brightness =>
            Json.obj("brightness_pct" -> brightness)
          }
          hassClient ! HomeAssistantClientBehavior.CallServiceRequest(
            domain = "light",
            service = s"turn_$state",
            entityId = entityId,
            replyTo = context.messageAdapter(ChangeLightStateResponse(intent.sessionId, intent.siteId, _)),
            serviceData = serviceData
          )
          Behaviors.same

        case ChangeLightStateResponse(sessionId, siteId, _) =>
          mqttClient ! MqttClientBehavior.Publish(audioServer.PlayBytesPayload(siteId, audioServer.AudioFile.Confirmation))
          mqttClient ! MqttClientBehavior.Publish(dialogueManager.EndSessionPayload(sessionId))
          Behaviors.same
      }
    }
}
