package hermesIntentHandler.intents

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import hermesIntentHandler.HermesIntent
import hermesIntentHandler.clients.MqttClientBehavior.{MqttClientMessage, Publish}
import play.api.libs.json.Json

import java.time.LocalTime

object GetTime {
  val IntentName = "GetTime"

  def apply(intent: HermesIntent, mqttClient: ActorRef[MqttClientMessage]): Behavior[HermesIntent] = Behaviors.setup { _ =>
    val now = LocalTime.now()
    val hour = if (now.getHour == 0 || now.getHour == 12) "12" else (now.getHour % 12).toString
    val minute = now.getMinute match {
      case 0           => "o clock"
      case m if m < 10 => s"o $m"
      case m           => m.toString
    }
    val response = Json.obj(
      "sessionId" -> intent.sessionId,
      "text" -> s"the time is $hour $minute"
    )
    mqttClient ! Publish("hermes/dialogueManager/endSession", response.toString().getBytes, qos = 2)

    Behaviors.stopped
  }
}
