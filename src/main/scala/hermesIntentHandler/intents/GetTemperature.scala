package hermesIntentHandler.intents

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import hermesIntentHandler.HermesIntent
import hermesIntentHandler.clients.HomeAssistantClientBehavior
import hermesIntentHandler.clients.MqttClientBehavior.{MqttClientMessage, Publish}
import play.api.libs.json.Json

object GetTemperature {
  val IntentName = "GetTemperature"
  private val WeatherEntityId = "weather.kdll_daynight"

  sealed trait GetTemperatureMessage
  private final case class GetWeatherStateResponse(response: HomeAssistantClientBehavior.StateResponse) extends GetTemperatureMessage

  def apply(intent: HermesIntent, mqttClient: ActorRef[MqttClientMessage]): Behavior[GetTemperatureMessage] = Behaviors.setup { context =>
    require(intent.intentName == IntentName)

    val homeAssistantClient = context.spawnAnonymous(HomeAssistantClientBehavior())
    homeAssistantClient ! HomeAssistantClientBehavior.GetStateRequest(WeatherEntityId, context.messageAdapter(GetWeatherStateResponse))

    Behaviors.receiveMessage { case GetWeatherStateResponse(response) =>
      val temperature = (response.attributes \ "temperature").as[Int]
      val temperatureStatement = Json.obj(
        "sessionId" -> intent.sessionId,
        "text" -> s"The temperature is currently $temperature degrees."
      )
      mqttClient ! Publish("hermes/dialogueManager/endSession", temperatureStatement.toString().getBytes, qos = 2)

      Behaviors.stopped
    }
  }
}
