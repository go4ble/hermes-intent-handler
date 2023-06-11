package hermesIntentHandler.intents

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import hermesIntentHandler.clients.{HomeAssistantClientBehavior, MqttClientBehavior}
import hermesIntentHandler.hermes.{IntentPayload, dialogueManager}
import org.eclipse.paho.client.mqttv3.MqttMessage

object GetWeatherBehavior {
  private val GetWeatherIntentTopic = "hermes/intent/GetWeather".r
  private val WeatherEntityId = "weather.kdll_daynight"

  sealed trait GetWeatherMessage
  private final case class HandleIntent(sessionId: String) extends GetWeatherMessage
  private object HandleIntent {
    def apply(mqttMessage: MqttMessage): HandleIntent = HandleIntent(IntentPayload.fromPayload(mqttMessage.getPayload).sessionId)
  }
  private final case class GetWeatherStateResponse(sessionId: String, response: HomeAssistantClientBehavior.StateResponse) extends GetWeatherMessage

  def apply(mqttClient: MqttClientBehavior.Actor, hassClient: HomeAssistantClientBehavior.Actor): Behavior[GetWeatherMessage] = Behaviors.setup { context =>
    mqttClient ! MqttClientBehavior.Subscribe(GetWeatherIntentTopic, context.messageAdapter { case (_, msg) => HandleIntent(msg) })

    Behaviors.receiveMessage {
      case HandleIntent(sessionId) =>
        hassClient ! HomeAssistantClientBehavior.GetStateRequest(WeatherEntityId, context.messageAdapter(GetWeatherStateResponse(sessionId, _)))
        Behaviors.same

      case GetWeatherStateResponse(sessionId, response) =>
        val forecast = response.attributes \ "forecast" \ 0
        val isDaytime = (forecast \ "daytime").as[Boolean]
        val when = if (isDaytime) "today" else "tonight"
        val detailedDescription = (forecast \ "detailed_description").as[String]
        // TODO time of day replacement (e.g. "1pm")
        val descriptionWithReplacements = detailedDescription.replaceAll("mph", "miles per hour")
        mqttClient ! MqttClientBehavior.Publish(dialogueManager.EndSessionPayload(sessionId, Some(s"The weather $when is $descriptionWithReplacements")))
        Behaviors.same
    }
  }
}
