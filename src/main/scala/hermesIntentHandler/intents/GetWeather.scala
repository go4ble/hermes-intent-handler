package hermesIntentHandler.intents

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import hermesIntentHandler.clients.HomeAssistantClientBehavior
import hermesIntentHandler.clients.MqttClientBehavior.MqttClientMessage
import hermesIntentHandler.hermes.{DialogueManager, HermesIntent}

object GetWeather {
  val IntentName = "GetWeather"
  private val WeatherEntityId = "weather.kdll_daynight"

  sealed trait GetWeatherMessage
  private final case class GetWeatherStateResponse(response: HomeAssistantClientBehavior.StateResponse) extends GetWeatherMessage

  def apply()(implicit intent: HermesIntent, mqttClient: ActorRef[MqttClientMessage]): Behavior[GetWeatherMessage] = Behaviors.setup { context =>
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
      DialogueManager.endSession(s"The weather $when is $descriptionWithReplacements")

      Behaviors.stopped
    }
  }
}
