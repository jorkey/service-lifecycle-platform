package com.vyulabs.update.distribution.graphql

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.model.ws._
import akka.stream.Materializer
import akka.stream.scaladsl._
import com.vyulabs.update.common.info.AccessToken
import org.slf4j.Logger
import sangria.parser.QueryParser
import spray.json.JsObject

import scala.concurrent.ExecutionContext
import scala.util._

trait GraphqlWebSocketSupport {
  protected val workspace: GraphqlWorkspace
  protected val graphql: Graphql

  case class Query(query: String, operation: Option[String], variables: JsObject)

  def handleWebSocket(accessToken: Option[AccessToken], tracing: Boolean)
                     (implicit system: ActorSystem, materializer: Materializer,
                      executionContext: ExecutionContext, log: Logger): Flow[Message, Message, Any] = {
    val output = MergeHub.source[TextMessage.Strict](perProducerBufferSize = 16)
    var sink = Option.empty[Sink[TextMessage.Strict, NotUsed]]
    val context = GraphqlContext(accessToken, workspace)

    val input = Flow[Message]
      .collect {
        case TextMessage.Strict(query) =>
          val graphqlQuery = query.asInstanceOf[Query]
          QueryParser.parse(graphqlQuery.query) match {
            case Success(document) =>
              val querySource = graphql.executeSubscriptionQueryToJsonSource(
                GraphqlSchema.SchemaDefinition, context, document, graphqlQuery.operation, graphqlQuery.variables, tracing)
              for (sink <- sink) {
                querySource.map(TextMessage.Strict(_)).to(sink).run()
              }
            case Failure(e) =>
              log.error("Graphql parse error: " + e.getMessage)
          }
        case message =>
          log.error("Unhandled WebSocket message: " + message)
      }
      .to(Sink.ignore)

    Flow.fromSinkAndSourceMat(input, output)((_, s) => { sink = Some(s); Unit })
  }
}
