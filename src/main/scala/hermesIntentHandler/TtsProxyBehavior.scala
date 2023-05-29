package hermesIntentHandler

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.unmarshalling.Unmarshal
import hermesIntentHandler.clients.MqttClientBehavior
import hermesIntentHandler.hermes.TtsSayPayload
import org.eclipse.paho.client.mqttv3.MqttMessage

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

object TtsProxyBehavior {
  sealed trait Message
  private final case class TtsSay(ttsSayPayload: TtsSayPayload) extends Message
  private final case class TtsResponse(siteId: String, audio: Try[Array[Byte]]) extends Message

  private val TtsSayTopic = "hermes/tts/say".r

  def apply(mqttClient: MqttClientBehavior.Actor): Behavior[Message] = Behaviors.setup { context =>
    implicit val system: ActorSystem[_] = context.system
    implicit val ec: ExecutionContext = context.executionContext

    val payloadAdapter = context.messageAdapter[(String, MqttMessage)] { case (_, mqttMessage) =>
      TtsSay(TtsSayPayload.fromPayload(mqttMessage.getPayload))
    }

    mqttClient ! MqttClientBehavior.Subscribe(TtsSayTopic, payloadAdapter)

    Behaviors.receiveMessage {
      case TtsSay(message) =>
        val request = HttpRequest(uri = s"${Config.tts.host}/api/tts?speaker_id=${Config.tts.speakerId}&text=${message.text}")
        context.pipeToSelf(httpGetBytes(request))(TtsResponse(message.siteId, _))
        Behaviors.same
      case TtsResponse(siteId, Success(audio)) =>
        mqttClient ! MqttClientBehavior.Publish(hermesAudioServerPlayBytesTopic(siteId), audio)
        Behaviors.same
      case TtsResponse(_, Failure(exception)) =>
        context.log.error("failed to retrieve TTS audio", exception)
        Behaviors.same
    }
  }

  private def hermesAudioServerPlayBytesTopic(siteId: String, requestId: String = UUID.randomUUID().toString) =
    s"hermes/audioServer/$siteId/playBytes/$requestId"

  private def httpGetBytes(request: HttpRequest)(implicit system: ActorSystem[_], ec: ExecutionContext): Future[Array[Byte]] = for {
    httpResponse <- Http().singleRequest(request)
    if httpResponse.status.isSuccess()
    responseBytes <- Unmarshal(httpResponse).to[Array[Byte]]
  } yield responseBytes
}
