package hermesIntentHandler.intents

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import hermesIntentHandler.HermesIntent
import hermesIntentHandler.clients.HomeAssistantClientBehavior
import hermesIntentHandler.clients.MqttClientBehavior.{MqttClientMessage, Publish}
import play.api.libs.json.Json

object GetWeather {
  val IntentName = "GetWeather"
  private val WeatherEntityId = "weather.kdll_daynight"

  sealed trait GetWeatherMessage
  private final case class GetWeatherStateResponse(response: HomeAssistantClientBehavior.StateResponse) extends GetWeatherMessage

  def apply(intent: HermesIntent, mqttClient: ActorRef[MqttClientMessage]): Behavior[GetWeatherMessage] = Behaviors.setup { context =>
    require(intent.intentName == IntentName)

    val homeAssistantClient = context.spawnAnonymous(HomeAssistantClientBehavior())
    homeAssistantClient ! HomeAssistantClientBehavior.GetStateRequest(WeatherEntityId, context.messageAdapter(GetWeatherStateResponse))

    Behaviors.receiveMessage { case GetWeatherStateResponse(response) =>
      val forecast = response.attributes \ "forecast" \ 0
      val isDaytime = (forecast \ "daytime").as[Boolean]
      val when = if (isDaytime) "today" else "tonight"
      val detailedDescription = (forecast \ "detailed_description").as[String]
      // TODO time of day replacement (e.g. "1pm")
      val descriptionWithReplacements = detailedDescription.replaceAll("mph", "miles per hour")
      val weatherStatement = Json.obj(
        "sessionId" -> intent.sessionId,
        "text" -> s"The weather $when is $descriptionWithReplacements"
      )
      mqttClient ! Publish("hermes/dialogueManager/endSession", weatherStatement.toString().getBytes, qos = 2)

      Behaviors.stopped
    }
  }
}
