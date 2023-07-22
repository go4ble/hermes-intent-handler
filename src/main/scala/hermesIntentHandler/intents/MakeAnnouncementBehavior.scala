package hermesIntentHandler.intents

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import hermesIntentHandler.clients.MqttClientBehavior
import hermesIntentHandler.hermes.{asr, audioServer, dialogueManager, nlu}
import org.eclipse.paho.client.mqttv3.MqttMessage

import java.util.UUID

object MakeAnnouncementBehavior {
  private val MakeAnnouncementIntentTopic = "hermes/intent/MakeAnnouncement".r

  sealed trait MakeAnnouncementMessage
  private final case class HandleIntent(intent: nlu.Intent) extends MakeAnnouncementMessage
  private object HandleIntent {
    def apply(payload: (String, MqttMessage)): HandleIntent = {
      val (_, message) = payload
      HandleIntent(nlu.Intent.fromPayload(message.getPayload))
    }
  }

  def apply(mqttClient: MqttClientBehavior.Actor): Behavior[MakeAnnouncementMessage] = Behaviors.setup { context =>
    mqttClient ! MqttClientBehavior.Subscribe(MakeAnnouncementIntentTopic, context.messageAdapter(HandleIntent(_)))

    Behaviors.receiveMessage { case HandleIntent(intent) =>
      context.spawnAnonymous(AnnouncementSessionBehavior(mqttClient, intent.siteId, intent.sessionId))
      mqttClient ! MqttClientBehavior.Publish(dialogueManager.EndSession(intent.sessionId, Some("What would you like to announce?")))
      Behaviors.same
    }
  }

  // TODO implement timeout
  private object AnnouncementSessionBehavior {
    sealed trait AnnouncementSessionMessage
    private final case class SessionEnded(sessionEnded: dialogueManager.SessionEnded) extends AnnouncementSessionMessage
    private object SessionEnded {
      def apply(payload: (String, MqttMessage)): SessionEnded = {
        val (_, message) = payload
        SessionEnded(dialogueManager.SessionEnded.fromPayload(message.getPayload))
      }
    }
    private final case class AudioCaptured(audio: Array[Byte]) extends AnnouncementSessionMessage
    private object AudioCaptured {
      def apply(payload: (String, MqttMessage)): AudioCaptured = {
        val (_, message) = payload
        AudioCaptured(message.getPayload)
      }
    }

    def apply(mqttClient: MqttClientBehavior.Actor, siteId: String, sessionId: String): Behavior[AnnouncementSessionMessage] = Behaviors.setup { context =>
      val sessionEndedSub = context.messageAdapter[(String, MqttMessage)](SessionEnded(_))
      mqttClient ! MqttClientBehavior.Subscribe(dialogueManager.SessionEnded.Topic.r, sessionEndedSub)

      val audioCapturedSub = context.messageAdapter[(String, MqttMessage)](AudioCaptured(_))

      Behaviors.receiveMessage {
        case SessionEnded(sessionEnded) =>
          println("foo") // TODO
          if (sessionEnded.sessionId == sessionId) {
            println("zip")
            val newSessionId = s"make-announcement-$siteId-${UUID.randomUUID()}"
            val audioCapturedTopic = s"hermes/asr/$siteId/$newSessionId/audioCaptured".r
            mqttClient ! MqttClientBehavior.Unsubscribe(sessionEndedSub)
            mqttClient ! MqttClientBehavior.Subscribe(audioCapturedTopic, audioCapturedSub)
            mqttClient ! MqttClientBehavior.Publish(
              asr.StartListening(
                siteId = siteId,
                sessionId = Some(newSessionId),
                sendAudioCaptured = true,
                intentFilter = Some(Seq("noop"))
              )
            )
          }
          Behaviors.same
// TODO there seems to still be an issue with subscriptions not staying segregated between actorRefs
        case AudioCaptured(audio) =>
          println("bar") // TODO
          mqttClient ! MqttClientBehavior.Publish(asr.StopListening(siteId, sessionId))
          // TODO broadcast rather than echo
          mqttClient ! MqttClientBehavior.Publish(audioServer.PlayBytes(siteId, audio, requestId = sessionId))
          mqttClient ! MqttClientBehavior.Unsubscribe(audioCapturedSub)
          Behaviors.stopped
      }
    }
  }
}
