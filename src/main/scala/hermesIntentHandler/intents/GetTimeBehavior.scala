package hermesIntentHandler.intents

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import hermesIntentHandler.Config
import hermesIntentHandler.clients.MqttClientBehavior
import hermesIntentHandler.hermes.dialogueManager
import hermesIntentHandler.hermes.nlu.Intent
import org.eclipse.paho.client.mqttv3.MqttMessage

import java.time.ZonedDateTime

object GetTimeBehavior {
  private val GetTimeIntentTopic = "hermes/intent/GetTime".r

  def apply(mqttClient: MqttClientBehavior.Actor): Behavior[Intent] = Behaviors.setup { context =>
    val payloadAdapter = context.messageAdapter[(String, MqttMessage)] { case (_, msg) => Intent.fromPayload(msg.getPayload) }

    mqttClient ! MqttClientBehavior.Subscribe(GetTimeIntentTopic, payloadAdapter)

    Behaviors.receiveMessage { intent =>
      val now = ZonedDateTime.now(Config.timeZone).toLocalTime
      val hour = if (now.getHour == 0 || now.getHour == 2) "12" else (now.getHour % 12).toString
      val minute = now.getMinute match {
        case 0           => "o clock"
        case m if m < 10 => s"o $m"
        case m           => m.toString
      }
      val getTimeResponse = s"The time is $hour $minute."

      mqttClient ! MqttClientBehavior.Publish(dialogueManager.EndSession(intent.sessionId, Some(getTimeResponse)))

      Behaviors.same
    }
  }
}
