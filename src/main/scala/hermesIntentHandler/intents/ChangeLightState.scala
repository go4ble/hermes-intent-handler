package hermesIntentHandler.intents

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import hermesIntentHandler.clients.HomeAssistantClientBehavior
import hermesIntentHandler.clients.MqttClientBehavior.MqttClientMessage
import hermesIntentHandler.hermes.{AudioServer, DialogueManager, HermesIntent}
import play.api.libs.json.Json

object ChangeLightState {
  val IntentName = "ChangeLightState"

  private val SlotNameLightName = "light_entity_id"
  private val SlotNameState = "state"
  private val SlotNameLightBrightnessPercentage = "light_brightness_percentage"

  sealed trait ChangeLightStateMessage
  private final case class ChangeLightStateResponse(response: Seq[HomeAssistantClientBehavior.StateResponse]) extends ChangeLightStateMessage

  def apply()(implicit
      intent: HermesIntent,
      mqttClient: ActorRef[MqttClientMessage],
      homeAssistantClient: ActorRef[HomeAssistantClientBehavior.HomeAssistantClientMessage]
  ): Behavior[ChangeLightStateMessage] = Behaviors.setup { context =>
    require(intent.intentName == IntentName)

    val entityId = intent.slots.find(_.slotName == SlotNameLightName).get.value.value.as[String]
    val state = intent.slots.find(_.slotName == SlotNameState).get.value.value.as[String]
    val serviceData = for {
      slot <- intent.slots.find(_.slotName == SlotNameLightBrightnessPercentage)
      brightness <- slot.value.value.asOpt[BigDecimal]
    } yield Json.obj("brightness_pct" -> brightness)
    homeAssistantClient ! HomeAssistantClientBehavior.CallServiceRequest(
      "light",
      s"turn_$state",
      entityId,
      context.messageAdapter(ChangeLightStateResponse),
      serviceData
    )

    Behaviors.receiveMessage { case ChangeLightStateResponse(_) =>
      AudioServer.playAudio(AudioServer.ConfirmationAudio)
      DialogueManager.endSession()

      Behaviors.stopped
    }
  }
}
