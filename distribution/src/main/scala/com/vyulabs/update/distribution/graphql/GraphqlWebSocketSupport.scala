package com.vyulabs.update.distribution.graphql

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.model.ws._
import akka.stream.scaladsl._
import akka.stream.{KillSwitches, Materializer, UniqueKillSwitch}
import org.slf4j.Logger
import sangria.parser.QueryParser
import spray.json._

import scala.concurrent.ExecutionContext
import scala.util._

trait GraphqlWebSocketSupport {
  protected val workspace: GraphqlWorkspace
  protected val graphql: Graphql

  def handleWebSocket(tracing: Boolean)
                     (implicit system: ActorSystem, materializer: Materializer,
                      executionContext: ExecutionContext, log: Logger): Flow[Message, Message, Any] = {
    val output = MergeHub.source[TextMessage.Strict](perProducerBufferSize = 16)
    var sink = Option.empty[Sink[TextMessage.Strict, NotUsed]]
    var context = Option.empty[GraphqlContext]

    @volatile
    var subscriptionKills = Map.empty[String, UniqueKillSwitch]

    if (log.isDebugEnabled) log.debug(s"Handle websocket")

    def serializeMessage[T <: OutputGraphqlMessage](message: T)(implicit writer: JsonWriter[T]) = {
      TextMessage.Strict(message.toJson.compactPrint)
    }

    def singleReply[T <: OutputGraphqlMessage](message: T)(implicit writer: JsonWriter[T]) = {
      for (sink <- sink) {
        if (log.isDebugEnabled) log.debug(s"Send websocket reply ${message}")
        Source.single(serializeMessage(message)).to(sink).run()
      }
    }

    def sendError(id: String, message: String): Unit = {
      if (log.isDebugEnabled) log.debug(s"Send websocket error for subscription ${id}: ${message}")
      log.error(message)
      singleReply(Error(id = id, payload = ErrorPayload(message)))
    }

    def handleMessage(message: String)(implicit log: Logger): Unit = {
      try {
        val query = message.parseJson.asJsObject
        if (log.isDebugEnabled) log.debug(s"Received websocket graphql query ${query}")
        val queryType = query.fields.get("type").map(_.asInstanceOf[JsString].value).getOrElse("unknown")
        queryType match {
          case ConnectionInit.`type` =>
            val init = query.convertTo[ConnectionInit]
            workspace.getOptionalAccessTokenFromHeader(init.payload.Authorization).onComplete {
              case Success(accessToken) =>
                context = Some(GraphqlContext(accessToken, None, workspace))
                singleReply(ConnectionAck())
              case Failure(ex) =>
                log.error("Getting access token error", ex)
            }
          case Subscribe.`type` =>
            val subscribe = query.convertTo[Subscribe]
            try {
              QueryParser.parse(subscribe.payload.query) match {
                case Success(document) =>
                  context match {
                    case Some(context) =>
                      val querySource = graphql.executeSubscriptionQueryToJsonSource(
                        GraphqlSchema.SchemaDefinition, context, document, subscribe.payload.operationName,
                        subscribe.payload.variables.getOrElse(JsObject.empty), tracing)
                      for (sink <- sink) {
                        val killSwitch = Source.combine(querySource
                          .flatMapConcat(msg => {
                            Source.single(serializeMessage(Next(id = subscribe.id, payload = msg.asJsObject))) }),
                          Source.single(serializeMessage(Complete(id = subscribe.id))))(Concat(_))
                          .map(msg => {
                            if (log.isDebugEnabled) log.debug(s"Send websocket message ${msg}"); msg
                          })
                          .watchTermination()( (_, _) => { subscriptionKills -= subscribe.id })
                          .viaMat(KillSwitches.single)(Keep.right)
                          .to(sink).run()
                        subscriptionKills += (subscribe.id -> killSwitch)
                      }
                    case None =>
                      sendError(subscribe.id, "Connection is not initialized")
                  }
                case Failure(ex) =>
                  sendError(subscribe.id, "Graphql parse error: " + ex.getMessage)
              }
            } catch {
              case ex: Throwable =>
                log.error("Subscribe query handle error", ex)
                sendError(subscribe.id, "Subscribe query handle error: " + ex.getMessage)
            }
          case Ping.`type` =>
            singleReply(Pong())
          case Complete.`type` =>
            val complete = query.convertTo[Complete]
            subscriptionKills.get(complete.`type`).foreach(_.shutdown())
          case _ =>
            log.error("Invalid query: " + query.compactPrint)
        }
      } catch {
        case ex: Exception =>
          log.error("Query handle error", ex)
      }
    }

    val input = Flow[Message]
      .collect {
        case TextMessage.Strict(message) =>
          handleMessage(message)
        case TextMessage.Streamed(source) =>
          source.runFold("")(_ + _).andThen {
            case Success(message) =>
              handleMessage(message)
            case Failure(ex) =>
              log.error("Receive WebSocket message error", ex)
          }
        case message =>
          log.error("Unhandled WebSocket message: " + message)
      }
      .to(Sink.ignore)

    Flow.fromSinkAndSourceCoupledMat(input, output)((_, s) => { sink = Some(s); Unit })
  }
}
