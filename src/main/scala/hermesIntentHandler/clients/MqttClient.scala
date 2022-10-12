package hermesIntentHandler.clients

import akka.Done
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import org.eclipse.paho.client.mqttv3
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

object MqttClient {
  final case class MqttMessage(topic: String, payload: Array[Byte])
  object MqttMessage {
    def apply(topic: String, message: mqttv3.MqttMessage): MqttMessage = MqttMessage(topic, message.getPayload)
  }

  sealed trait MqttClientMessage
  final case class Subscribe(topic: String, replyTo: ActorRef[MqttMessage]) extends MqttClientMessage
  final case class Unsubscribe(topic: String) extends MqttClientMessage
  // TODO qos enum
  final case class Publish(topic: String, payload: Array[Byte], qos: Int) extends MqttClientMessage
  final case class Disconnect(replyTo: ActorRef[Done]) extends MqttClientMessage

  def apply(): Behavior[MqttClientMessage] = Behaviors.setup { context =>
    val mqttBroker = sys.env.getOrElse("HERMES_INTENT_HANDLER_MQTT_BROKER", "tcp://localhost:1883")
    context.log.info(s"connecting to MQTT broker at $mqttBroker")
    val mqttClient = new mqttv3.MqttClient(mqttBroker, mqttv3.MqttClient.generateClientId(), new MemoryPersistence)
    mqttClient.connect()

    Behaviors.receiveMessage {
      case Subscribe(topic, replyTo) =>
        mqttClient.subscribe(topic, (subTopic: String, subMessage: mqttv3.MqttMessage) => {
          replyTo ! MqttMessage(subTopic, subMessage)
        })
        Behaviors.same

      case Unsubscribe(topic) =>
        ???
        Behaviors.same

      case Publish(topic, payload, qos) =>
        ???
        Behaviors.same

      case Disconnect(replyTo) =>
        mqttClient.disconnect()
        replyTo ! Done
        Behaviors.stopped
    }
  }
}
