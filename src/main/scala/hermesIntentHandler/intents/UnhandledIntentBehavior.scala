package hermesIntentHandler.intents

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import hermesIntentHandler.clients.MqttClientBehavior
import hermesIntentHandler.hermes.{IntentPayload, dialogueManager}
import org.eclipse.paho.client.mqttv3.MqttMessage

object UnhandledIntentBehavior {
  def apply(mqttClient: MqttClientBehavior.Actor): Behavior[(String, MqttMessage)] = Behaviors.setup { context =>
    mqttClient ! MqttClientBehavior.SubscribeToUnhandled(context.self)

    Behaviors.receiveMessage {
      case (topic, mqttMessage) if topic.startsWith("hermes/intent/") =>
        val intent = IntentPayload.fromPayload(mqttMessage.getPayload)
        val intentNameHuman = intent.intentName.replaceAll("([A-Z])", " $1").trim
        val response = s"Sorry. Nothing is configured to handle the $intentNameHuman intent."
        context.log.warn(s"Unhandled intent: ${intent.intentName}")
        mqttClient ! MqttClientBehavior.Publish(
          dialogueManager.EndSessionTopic,
          dialogueManager.EndSessionPayload(intent.sessionId, Some(response))
        )
        Behaviors.same
      case _ =>
        Behaviors.same
    }
  }
}
