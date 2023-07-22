package hermesIntentHandler

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.unmarshalling.Unmarshal
import hermesIntentHandler.clients.MqttClientBehavior
import hermesIntentHandler.hermes.tts.Say
import org.eclipse.paho.client.mqttv3.MqttMessage

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

object TtsProxyBehavior {
  sealed trait Message
  private final case class TtsSay(ttsSayPayload: Say) extends Message
  private final case class TtsResponse(ttsSayPayload: Say, audio: Try[Array[Byte]]) extends Message

  private val TtsSayTopic = "hermes/tts/say".r

  def apply(mqttClient: MqttClientBehavior.Actor): Behavior[Message] = Behaviors.setup { context =>
    implicit val system: ActorSystem[_] = context.system
    implicit val ec: ExecutionContext = context.executionContext

    val payloadAdapter = context.messageAdapter[(String, MqttMessage)] { case (_, mqttMessage) =>
      TtsSay(Say.fromPayload(mqttMessage.getPayload))
    }

    mqttClient ! MqttClientBehavior.Subscribe(TtsSayTopic, payloadAdapter)

    Behaviors.receiveMessage {
      case TtsSay(message) =>
        val text = URLEncoder.encode(message.text, StandardCharsets.UTF_8)
        val request = HttpRequest(uri = s"${Config.tts.host}/api/tts?speaker_id=${Config.tts.speakerId}&text=$text")
        context.pipeToSelf(httpGetBytes(request))(TtsResponse(message, _))
        Behaviors.same
      case TtsResponse(sayPayload, Success(audio)) =>
        val sayId = sayPayload.id.getOrElse(UUID.randomUUID().toString)
        mqttClient ! MqttClientBehavior.Publish(hermes.audioServer.PlayBytes(sayPayload.siteId, audio, sayId))
        mqttClient ! MqttClientBehavior.Publish(hermes.tts.SayFinished(sayPayload))
        Behaviors.same
      case TtsResponse(_, Failure(exception)) =>
        context.log.error("failed to retrieve TTS audio", exception)
        Behaviors.same
    }
  }

  private def httpGetBytes(request: HttpRequest)(implicit system: ActorSystem[_], ec: ExecutionContext): Future[Array[Byte]] = for {
    httpResponse <- Http().singleRequest(request)
    if httpResponse.status.isSuccess()
    responseBytes <- Unmarshal(httpResponse).to[Array[Byte]]
  } yield responseBytes
}
