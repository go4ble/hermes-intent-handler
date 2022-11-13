package hermesIntentHandler.clients

import akka.Done
import akka.actor.CoordinatedShutdown
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import hermesIntentHandler.Config
import org.eclipse.paho.client.mqttv3
import org.eclipse.paho.client.mqttv3.IMqttMessageListener
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

import scala.concurrent.duration._

object MqttClientBehavior {
  final case class MqttMessage(topic: String, payload: Array[Byte])
  object MqttMessage {
    def apply(topic: String, message: mqttv3.MqttMessage): MqttMessage = MqttMessage(topic, message.getPayload)
  }

  sealed trait MqttClientMessage
  // TODO qos enum
  final case class Publish(topic: String, payload: Array[Byte], qos: Int) extends MqttClientMessage
  final case class Disconnect(replyTo: ActorRef[Done]) extends MqttClientMessage

  def apply(replyTo: ActorRef[MqttMessage], topic: String): Behavior[MqttClientMessage] = Behaviors.setup { context =>
    val clientId = mqttv3.MqttClient.generateClientId()
    context.log.info(s"connecting to MQTT broker at ${Config.mqtt.broker} as $clientId subscribed to $topic")
    val mqttClient = new mqttv3.MqttClient(Config.mqtt.broker, clientId, new MemoryPersistence)
    mqttClient.connect()
    val messageListener = new IMqttMessageListener {
      override def messageArrived(topic: String, message: mqttv3.MqttMessage): Unit =
        replyTo ! MqttMessage(topic, message)
    }
    mqttClient.subscribe(topic, messageListener)

    val shutdownTaskName = s"mqtt-client-shutdown-$clientId"
    CoordinatedShutdown(context.system).addTask(CoordinatedShutdown.PhaseBeforeServiceUnbind, shutdownTaskName)(() => {
      context.self.ask(Disconnect)(5.seconds, context.system.scheduler)
    })

    Behaviors.receiveMessage {
      case Publish(topic, payload, qos) =>
        mqttClient.publish(topic, payload, qos, false)
        Behaviors.same

      case Disconnect(replyTo) =>
        mqttClient.disconnect()
        replyTo ! Done
        Behaviors.stopped
    }
  }
}
