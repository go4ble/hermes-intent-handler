package hermesIntentHandler

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import hermesIntentHandler.clients.{HomeAssistantClientBehavior, MqttClientBehavior}

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.util.chaining._

object App extends scala.App {
  private val actorSystem = ActorSystem(
    Behaviors.setup[Unit] { context =>
      def spawnAndWatch[T](behavior: Behavior[T], name: String): ActorRef[T] =
        context.spawn(behavior, name).tap(context.watch)

      val mqttClient = spawnAndWatch(MqttClientBehavior(), "mqtt-client")
      val hassClient = spawnAndWatch(HomeAssistantClientBehavior(), "hass-client")
      Seq(
        "tts-proxy" -> TtsProxyBehavior(mqttClient),
        "intents.get-time" -> intents.GetTimeBehavior(mqttClient),
        "intents.get-temperature" -> intents.GetTemperatureBehavior(mqttClient, hassClient),
        "intents.get-weather" -> intents.GetWeatherBehavior(mqttClient, hassClient),
        "intents.change-light-state" -> intents.ChangeLightStateBehavior(mqttClient, hassClient),
        "intents.unhandled-intent" -> intents.UnhandledIntentBehavior(mqttClient)
      ).foreach { case (name, behavior) => spawnAndWatch(behavior, name) }

      Behaviors.empty
    },
    "app"
  )

  Await.result(actorSystem.whenTerminated, Duration.Inf)
}
