package hermesIntentHandler.clients

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.Materializer
import hermesIntentHandler.Config
import play.api.libs.json._

import java.time.OffsetDateTime
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

object HomeAssistantClientBehavior {
  type Actor = ActorRef[HomeAssistantClientMessage]

  private val defaultJsonConfiguration = JsonConfiguration(naming = JsonNaming.SnakeCase)

  sealed trait HomeAssistantClientMessage

  final case class GetStateRequest(entityId: String, replyTo: ActorRef[StateResponse]) extends HomeAssistantClientMessage
  final case class CallServiceRequest(
      domain: String,
      service: String,
      entityId: String,
      replyTo: ActorRef[Seq[StateResponse]],
      serviceData: Option[JsObject] = None
  ) extends HomeAssistantClientMessage

  private final case class StateResponseInternal(getStateResponse: Try[StateResponse], replyTo: ActorRef[StateResponse]) extends HomeAssistantClientMessage
  private final case class StateResponsesInternal(stateResponses: Try[Seq[StateResponse]], replyTo: ActorRef[Seq[StateResponse]])
      extends HomeAssistantClientMessage

  final case class StateResponse(entityId: String, state: String, lastChanged: OffsetDateTime, lastUpdated: OffsetDateTime, attributes: JsObject)
  implicit val stateResponseReads: Reads[StateResponse] = Json.configured(defaultJsonConfiguration).reads

  private final case class CallServiceRequestBody(entityId: String, serviceData: Option[JsObject])
  private implicit val callServiceRequestBodyWrites: Writes[CallServiceRequestBody] = { case CallServiceRequestBody(entityId, serviceData) =>
    serviceData.getOrElse(JsObject.empty) + ("entity_id" -> JsString(entityId))
  }

  def apply(): Behavior[HomeAssistantClientMessage] = Behaviors.setup { context =>
    implicit val system: ActorSystem[_] = context.system
    implicit val ec: ExecutionContext = context.executionContext

    Behaviors.receiveMessage {
      case GetStateRequest(entityId, replyTo) =>
        val request = HttpRequest(
          uri = s"${Config.hass.host}/api/states/$entityId",
          headers = Seq(headers.Authorization(headers.OAuth2BearerToken(Config.hass.token)))
        )
        context.pipeToSelf(Http().singleRequest(request).asJson[StateResponse])(StateResponseInternal(_, replyTo))
        Behaviors.same

      case CallServiceRequest(domain, service, entityId, replyTo, serviceData) =>
        val request = HttpRequest(
          uri = s"${Config.hass.host}/api/services/$domain/$service",
          headers = Seq(headers.Authorization(headers.OAuth2BearerToken(Config.hass.token))),
          method = HttpMethods.POST,
          entity = HttpEntity(ContentTypes.`application/json`, Json.toJson(CallServiceRequestBody(entityId, serviceData)).toString())
        )
        context.pipeToSelf(Http().singleRequest(request).asJson[Seq[StateResponse]])(StateResponsesInternal(_, replyTo))
        Behaviors.same

      case StateResponseInternal(Success(response), replyTo) =>
        replyTo ! response
        Behaviors.same

      case StateResponseInternal(Failure(exception), _) =>
        context.log.error("failed making request to get state", exception)
        Behaviors.same

      case StateResponsesInternal(Success(response), replyTo) =>
        replyTo ! response
        Behaviors.same

      case StateResponsesInternal(Failure(exception), _) =>
        context.log.error("failed making request to get states", exception)
        Behaviors.same
    }
  }

  implicit class EnhancedHttpResponse(httpResponseF: Future[HttpResponse]) {
    def asJson[T](implicit reads: Reads[T], mat: Materializer, ec: ExecutionContext): Future[T] = for {
      httpResponse <- httpResponseF
      _ = require(httpResponse.status.isSuccess())
      responseBytes <- Unmarshal(httpResponse).to[Array[Byte]]
      response <- Future.fromTry(Try(Json.parse(responseBytes).as[T]))
    } yield response
  }
}
