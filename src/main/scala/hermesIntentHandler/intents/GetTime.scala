package hermesIntentHandler.intents

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import hermesIntentHandler.clients.MqttClientBehavior.MqttClientMessage
import hermesIntentHandler.hermes.{DialogueManager, HermesIntent}

import java.time.LocalTime

object GetTime {
  val IntentName = "GetTime"

  def apply()(implicit intent: HermesIntent, mqttClient: ActorRef[MqttClientMessage]): Behavior[HermesIntent] = Behaviors.setup { _ =>
    val now = LocalTime.now()
    val hour = if (now.getHour == 0 || now.getHour == 12) "12" else (now.getHour % 12).toString
    val minute = now.getMinute match {
      case 0           => "o clock"
      case m if m < 10 => s"o $m"
      case m           => m.toString
    }
    DialogueManager.endSession(s"the time is $hour $minute")

    Behaviors.stopped
  }
}
