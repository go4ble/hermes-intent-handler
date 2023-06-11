package hermesIntentHandler.hermes.audioServer

import hermesIntentHandler.MqttPayload
import java.util.UUID

case class PlayBytesPayload(siteId: String, audioBytes: Array[Byte], requestId: String = UUID.randomUUID().toString) extends MqttPayload {
  override val topic: String = s"hermes/audioServer/$siteId/playBytes/$requestId"
  override val payload: Array[Byte] = audioBytes
}

object PlayBytesPayload {
  def apply(siteId: String, audioFile: AudioFile): PlayBytesPayload =
    PlayBytesPayload(siteId, audioFile.getBytes)
}
