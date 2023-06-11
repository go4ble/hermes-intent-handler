package hermesIntentHandler.hermes.audioServer

import org.apache.commons.io.IOUtils

sealed trait AudioFile {
  protected val resource: String
  lazy val getBytes: Array[Byte] = IOUtils.resourceToByteArray(resource)
}

object AudioFile {
  final object Confirmation extends AudioFile {
    // TODO occasionally chose random audio to play (https://www.myinstants.com/en/favorites/)
    override protected val resource: String = "/discord-notification.wav"
  }
}
