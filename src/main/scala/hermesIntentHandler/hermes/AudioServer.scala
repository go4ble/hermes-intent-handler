package hermesIntentHandler.hermes

import akka.actor.typed.ActorRef
import hermesIntentHandler.clients.MqttClientBehavior.{MqttClientMessage, Publish}
import org.apache.commons.io.IOUtils

import java.util.UUID

object AudioServer {
  def playAudio(audioFile: AudioFile)(implicit intent: HermesIntent, mqttClient: ActorRef[MqttClientMessage]): Unit =
    mqttClient ! Publish(playBytesTopic(intent.siteId), audioFile.resourceBytes, qos = 2)

  private def playBytesTopic(siteId: String): String =
    s"hermes/audioServer/$siteId/playBytes/${UUID.randomUUID()}"

  sealed trait AudioFile {
    protected val resource: String
    protected[AudioServer] val resourceBytes: Array[Byte] = IOUtils.resourceToByteArray(resource)
  }
  final object ConfirmationAudio extends AudioFile {
    // TODO occasionally chose random audio to play (https://www.myinstants.com/en/favorites/)
    override protected val resource: String = "/discord-notification.wav"
  }
}
