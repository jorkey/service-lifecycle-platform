package com.vyulabs.update.distribution.graphql

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.model.ws._
import akka.stream.Materializer
import akka.stream.scaladsl._
import com.vyulabs.update.common.info.AccessToken
import org.slf4j.Logger
import sangria.parser.QueryParser
import spray.json._

import scala.concurrent.ExecutionContext
import scala.util._

trait GraphqlWebSocketSupport {
  protected val workspace: GraphqlWorkspace
  protected val graphql: Graphql

  def handleWebSocket(accessToken: Option[AccessToken], tracing: Boolean)
                     (implicit system: ActorSystem, materializer: Materializer,
                      executionContext: ExecutionContext, log: Logger): Flow[Message, Message, Any] = {
    val output = MergeHub.source[TextMessage.Strict](perProducerBufferSize = 16)
    var sink = Option.empty[Sink[TextMessage.Strict, NotUsed]]
    val context = GraphqlContext(accessToken, workspace)

    def serializeMessage[T <: OutputGraphqlMessage](message: T)(implicit writer: JsonWriter[T]) = {
      TextMessage.Strict(message.toJson.compactPrint)
    }

    def singleReply[T <: OutputGraphqlMessage](message: T)(implicit writer: JsonWriter[T]) = {
      for (sink <- sink) {
        Source.single(serializeMessage(message)).to(sink).run()
      }
    }

    def sendError(id: String, message: String): Unit = {
      log.error(message)
      singleReply(Error(id = id, payload = ErrorPayload(message)))
    }

    val input = Flow[Message]
      .collect {
        case TextMessage.Strict(queryMessage) =>
          try {
            val query = queryMessage.parseJson.asJsObject
            val queryType = query.fields.get("type").map(_.asInstanceOf[JsString].value).getOrElse("unknown")
            queryType match {
              case ConnectionInit.`type` =>
                val init = query.convertTo[ConnectionInit]
                singleReply(ConnectionAck())
              case Subscribe.`type` =>
                val subscribe = query.convertTo[Subscribe]
                QueryParser.parse(subscribe.payload.query) match {
                  case Success(document) =>
                    val querySource = graphql.executeSubscriptionQueryToJsonSource(
                      GraphqlSchema.SchemaDefinition, context, document, subscribe.payload.operationName,
                      subscribe.payload.variables.getOrElse(JsObject.empty), tracing)
                    for (sink <- sink) {
                      querySource.map(msg => serializeMessage(
                        Next(id = subscribe.id, payload = ExecutionResult(msg)))).to(sink).run()
                    }
                  case Failure(e) =>
                    sendError(subscribe.id, "Graphql parse error: " + e.getMessage)
                }
              case Ping.`type` =>
                singleReply(Pong())
              case _ =>
                log.error("Invalid query: " + query.compactPrint)
            }
          } catch {
            case ex: Exception =>
              log.error("Query handle error", ex)
          }
        case message =>
          log.error("Unhandled WebSocket message: " + message)
      }
      .to(Sink.ignore)

    Flow.fromSinkAndSourceMat(input, output)((_, s) => { sink = Some(s); Unit })
  }
}
