package hermesIntentHandler

import akka.Done
import akka.actor.CoordinatedShutdown
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem}
import hermesIntentHandler.clients.MqttClient
import hermesIntentHandler.clients.MqttClient.MqttMessage
import play.api.libs.json.Json

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.{Success, Try}

object App extends scala.App {
  sealed trait AppMessage
  private final case class Shutdown(replyTo: ActorRef[Done]) extends AppMessage
  private final case class LogMessage(message: MqttMessage) extends AppMessage

  val actorSystem = ActorSystem(
    Behaviors.setup[AppMessage] { context =>
      val mqttClient = context.spawn(MqttClient(), "mqtt-client")
      context.watch(mqttClient)

      if (context.log.isDebugEnabled) {
        mqttClient ! MqttClient.Subscribe("#", context.messageAdapter(LogMessage))
      }

      Behaviors.receiveMessage {
        case Shutdown(replyTo) =>
          context.log.info("shutting down...")
          mqttClient ! MqttClient.Disconnect(replyTo)
          Behaviors.stopped

        case LogMessage(MqttClient.MqttMessage(topic, payload)) =>
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

  CoordinatedShutdown(actorSystem).addTask(CoordinatedShutdown.PhaseBeforeServiceUnbind, "shutdown") { () =>
    actorSystem.ask(Shutdown)(15.seconds, actorSystem.scheduler)
  }

  Await.result(actorSystem.whenTerminated, Duration.Inf)
}
