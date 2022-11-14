package hermesIntentHandler.intents

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import hermesIntentHandler.clients.HomeAssistantClientBehavior
import hermesIntentHandler.clients.MqttClientBehavior.MqttClientMessage
import hermesIntentHandler.hermes.{AudioServer, DialogueManager, HermesIntent}

object ChangeLightState {
  val IntentName = "ChangeLightState"

  private val SlotNameLightName = "light_entity_id"
  private val SlotNameState = "state"

  sealed trait ChangeLightStateMessage
  private final case class ChangeLightStateResponse(response: Seq[HomeAssistantClientBehavior.StateResponse]) extends ChangeLightStateMessage

  def apply()(implicit
      intent: HermesIntent,
      mqttClient: ActorRef[MqttClientMessage],
      homeAssistantClient: ActorRef[HomeAssistantClientBehavior.HomeAssistantClientMessage]
  ): Behavior[ChangeLightStateMessage] = Behaviors.setup { context =>
    require(intent.intentName == IntentName)

    val entityId = intent.getSlotValue(SlotNameLightName)
    val state = intent.getSlotValue(SlotNameState)

    homeAssistantClient ! HomeAssistantClientBehavior.CallServiceRequest("light", s"turn_$state", entityId, context.messageAdapter(ChangeLightStateResponse))

    Behaviors.receiveMessage { case ChangeLightStateResponse(_) =>
      AudioServer.playAudio(AudioServer.ConfirmationAudio)
      DialogueManager.endSession()

      Behaviors.stopped
    }
  }
}
