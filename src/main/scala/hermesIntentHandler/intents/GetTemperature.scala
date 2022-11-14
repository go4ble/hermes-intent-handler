package hermesIntentHandler.intents

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import hermesIntentHandler.clients.HomeAssistantClientBehavior
import hermesIntentHandler.clients.MqttClientBehavior.MqttClientMessage
import hermesIntentHandler.hermes.{DialogueManager, HermesIntent}

object GetTemperature {
  val IntentName = "GetTemperature"
  private val WeatherEntityId = "weather.kdll_daynight"

  sealed trait GetTemperatureMessage
  private final case class GetWeatherStateResponse(response: HomeAssistantClientBehavior.StateResponse) extends GetTemperatureMessage

  def apply()(implicit
      intent: HermesIntent,
      mqttClient: ActorRef[MqttClientMessage],
      homeAssistantClient: ActorRef[HomeAssistantClientBehavior.HomeAssistantClientMessage]
  ): Behavior[GetTemperatureMessage] = Behaviors.setup { context =>
    require(intent.intentName == IntentName)

    homeAssistantClient ! HomeAssistantClientBehavior.GetStateRequest(WeatherEntityId, context.messageAdapter(GetWeatherStateResponse))

    Behaviors.receiveMessage { case GetWeatherStateResponse(response) =>
      val temperature = (response.attributes \ "temperature").as[Int]
      DialogueManager.endSession(s"The temperature is currently $temperature degrees.")

      Behaviors.stopped
    }
  }
}
