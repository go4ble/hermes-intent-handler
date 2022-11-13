package hermesIntentHandler

import akka.actor.typed.{ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import hermesIntentHandler.clients.{HomeAssistantClientBehavior, MqttClientBehavior}
import play.api.libs.json.Json

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

object App extends scala.App {
  sealed trait AppMessage
  private final case class MqttMessageReceived(message: MqttClientBehavior.MqttMessage) extends AppMessage
  private final case class HermesIntentReceived(intent: HermesIntent) extends AppMessage
  private final case class LogMessage(message: MqttClientBehavior.MqttMessage) extends AppMessage

  val actorSystem = ActorSystem(
    Behaviors.setup[AppMessage] { context =>
      val mqttClient = context.spawn(MqttClientBehavior(context.messageAdapter(MqttMessageReceived), "#"), "intents-mqtt-client")
      context.watch(mqttClient)
      val homeAssistantClient = context.spawn(HomeAssistantClientBehavior(), "home-assistant-client")
      context.watch(homeAssistantClient)

      Behaviors.receiveMessage {
        case MqttMessageReceived(mqttMessage) =>
          if (context.log.isDebugEnabled) {
            context.self ! LogMessage(mqttMessage)
          }
          if (mqttMessage.topic startsWith "hermes/intent/")
            Try(Json.parse(mqttMessage.payload).as[HermesIntent]) match {
              case Success(hermesIntent) =>
                context.self ! HermesIntentReceived(hermesIntent)
              case Failure(exception) =>
                context.log.error(s"failed to parse intent: ${mqttMessage.topic}", exception)
            }
          Behaviors.same

        case HermesIntentReceived(intent) =>
          val behavior: Behavior[_] = intent.intentName match {
            case intents.GetTime.IntentName          => intents.GetTime(intent, mqttClient)
            case intents.GetWeather.IntentName       => intents.GetWeather(intent, mqttClient)
            case intents.GetTemperature.IntentName   => intents.GetTemperature(intent, mqttClient)
            case intents.ChangeLightState.IntentName => intents.ChangeLightState(intent, mqttClient)
            case _ =>
              context.log.error(s"unknown intent: $intent")
              Behaviors.empty
          }
          context.spawnAnonymous(behavior) // TODO terminate after 60 seconds
          Behaviors.same

        case LogMessage(MqttClientBehavior.MqttMessage(topic, payload)) =>
          Try { Json.parse(payload) } match {
            case Success(json) =>
              val jsonString = Json.prettyPrint(json).replaceAll("\n", "\n  ").trim
              context.log.debug(s"$topic:\n  $jsonString")
            case _ =>
              context.log.debug(s"$topic: ***")
          }
          Behaviors.same
      }
    },
    "app"
  )

  Await.result(actorSystem.whenTerminated, Duration.Inf)
}
