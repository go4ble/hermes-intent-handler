package hermesIntentHandler.clients

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import hermesIntentHandler.{Config, MqttPayload}
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import org.eclipse.paho.client.mqttv3.{MqttClient, MqttMessage}
import play.api.libs.json.Json

import scala.util.Try
import scala.util.matching.Regex

object MqttClientBehavior {
  // TODO CoordinatedShutdown
  type Actor = ActorRef[Message]

  sealed trait Message
  final case class Subscribe(topic: Regex, replyTo: ActorRef[(String, MqttMessage)]) extends Message
  final case class SubscribeToUnhandled(replyTo: ActorRef[(String, MqttMessage)]) extends Message
  final case class Unsubscribe(replyTo: ActorRef[(String, MqttMessage)]) extends Message
  final case class Publish(mqttPayload: MqttPayload) extends Message
  private final case class MessageReceived(topic: String, message: MqttMessage) extends Message

  def apply(): Behavior[Message] = Behaviors.setup { context =>
    val clientId = MqttClient.generateClientId()
    context.log.info(s"connecting to MQTT broker at ${Config.mqtt.broker} as $clientId")
    val mqttClient = new MqttClient(Config.mqtt.broker, clientId, new MemoryPersistence) // TODO executor service
    mqttClient.connect()
    mqttClient.subscribe("#", context.self ! MessageReceived(_, _))
    apply(mqttClient, Map.empty)
  }

  private def apply(client: MqttClient, subscriptions: Map[ActorRef[(String, MqttMessage)], Either[Unit, Regex]]): Behavior[Message] = Behaviors.setup {
    context =>
      lazy val unhandledMessageSubscriptions = subscriptions.collect { case (replyTo, Left(())) => replyTo }
      Behaviors.receiveMessage {
        case Subscribe(topic, replyTo) =>
          context.log.debug(s"Subscribe: $topic, $replyTo")
          apply(client, subscriptions + (replyTo -> Right(topic)))
        case SubscribeToUnhandled(replyTo) =>
          context.log.debug(s"SubscribeToUnhandled: $replyTo")
          apply(client, subscriptions + (replyTo -> Left(())))
        case Unsubscribe(replyTo) =>
          context.log.debug(s"Unsubscribe: $replyTo")
          apply(client, subscriptions - replyTo)
        case Publish(mqttPayload) =>
          client.publish(mqttPayload.topic, mqttPayload.payload, mqttPayload.qos, mqttPayload.retained)
          Behaviors.same
        case MessageReceived(topic, message) =>
          val payloadText = Try(Json.parse(message.getPayload)).map(_.toString()).getOrElse(s"[${message.getPayload.length} bytes]")
          context.log.debug(s"MessageReceived: $topic, ${payloadText.take(500)}")
          val matchedSubscribers = subscriptions.collect { case (replyTo, Right(pattern)) if pattern.matches(topic) => replyTo }
          val sendTo = if (matchedSubscribers.isEmpty) unhandledMessageSubscriptions else matchedSubscribers
          sendTo.foreach(_ ! (topic, message))
          Behaviors.same
      }
  }
}
