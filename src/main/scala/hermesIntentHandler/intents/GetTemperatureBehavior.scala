package hermesIntentHandler.intents

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import hermesIntentHandler.clients.{HomeAssistantClientBehavior, MqttClientBehavior}
import org.eclipse.paho.client.mqttv3.MqttMessage
import hermesIntentHandler.hermes.dialogueManager
import hermesIntentHandler.hermes.nlu.Intent

object GetTemperatureBehavior {
  private val GetTemperatureIntentTopic = "hermes/intent/GetTemperature".r
  private val WeatherEntityId = "weather.kdll_daynight"

  sealed trait GetTemperatureMessage
  private final case class HandleIntent(sessionId: String) extends GetTemperatureMessage
  private object HandleIntent {
    def apply(mqttMessage: MqttMessage): HandleIntent = HandleIntent(Intent.fromPayload(mqttMessage.getPayload).sessionId)
  }
  private final case class GetWeatherStateResponse(sessionId: String, response: HomeAssistantClientBehavior.StateResponse) extends GetTemperatureMessage

  def apply(mqttClient: MqttClientBehavior.Actor, hassClient: HomeAssistantClientBehavior.Actor): Behavior[GetTemperatureMessage] = Behaviors.setup { context =>
    mqttClient ! MqttClientBehavior.Subscribe(GetTemperatureIntentTopic, context.messageAdapter { case (_, msg) => HandleIntent(msg) })

    Behaviors.receiveMessage {
      case HandleIntent(sessionId) =>
        hassClient ! HomeAssistantClientBehavior.GetStateRequest(WeatherEntityId, context.messageAdapter(GetWeatherStateResponse(sessionId, _)))
        Behaviors.same

      case GetWeatherStateResponse(sessionId, response) =>
        val temperature = (response.attributes \ "temperature").as[Int]
        mqttClient ! MqttClientBehavior.Publish(dialogueManager.EndSession(sessionId, Some(s"The temperature is currently $temperature degrees.")))
        Behaviors.same
    }
  }
}
