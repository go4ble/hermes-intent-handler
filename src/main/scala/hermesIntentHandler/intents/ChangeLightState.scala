package hermesIntentHandler.intents

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import hermesIntentHandler.HermesIntent
import hermesIntentHandler.clients.HomeAssistantClientBehavior
import hermesIntentHandler.clients.MqttClientBehavior.{MqttClientMessage, Publish}
import org.apache.commons.io.IOUtils
import play.api.libs.json.Json

import java.util.UUID

object ChangeLightState {
  val IntentName = "ChangeLightState"

  private val SlotNameLightName = "light_entity_id"
  private val SlotNameState = "state"
  private val ConfirmationAudio = IOUtils.resourceToByteArray("/discord-notification.wav")

  sealed trait ChangeLightStateMessage
  private final case class ChangeLightStateResponse(response: Seq[HomeAssistantClientBehavior.StateResponse]) extends ChangeLightStateMessage

  def apply(intent: HermesIntent, mqttClient: ActorRef[MqttClientMessage]): Behavior[ChangeLightStateMessage] = Behaviors.setup { context =>
    require(intent.intentName == IntentName)

    val entityId = intent.getSlotValue(SlotNameLightName)
    val state = intent.getSlotValue(SlotNameState)

    val homeAssistantClient = context.spawnAnonymous(HomeAssistantClientBehavior())
    homeAssistantClient ! HomeAssistantClientBehavior.CallServiceRequest("light", s"turn_$state", entityId, context.messageAdapter(ChangeLightStateResponse))

    Behaviors.receiveMessage { case ChangeLightStateResponse(_) =>
      // TODO occasionally chose random audio to play (https://www.myinstants.com/en/favorites/)
      val playAudioTopic = s"hermes/audioServer/${intent.siteId}/playBytes/${UUID.randomUUID()}"
      mqttClient ! Publish(playAudioTopic, ConfirmationAudio, qos = 2)
      val endSession = Json.obj("sessionId" -> intent.sessionId)
      mqttClient ! Publish("hermes/dialogueManager/endSession", endSession.toString().getBytes, qos = 2)

      Behaviors.stopped
    }
  }
}
