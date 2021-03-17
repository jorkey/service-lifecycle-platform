package com.vyulabs.update.distribution.graphql

import sangria.execution.{FieldTag, MiddlewareBeforeField, MiddlewareQueryContext}
import sangria.schema.Context

case object Authorized extends FieldTag

case class AuthException(message: String) extends Exception(message)

object AuthMiddleware extends MiddlewareBeforeField[GraphqlContext] {
  type QueryVal = Unit
  type FieldVal = Unit

  def beforeQuery(context: MiddlewareQueryContext[GraphqlContext, _, _]) = {}

  def afterQuery(queryVal: QueryVal, context: MiddlewareQueryContext[GraphqlContext, _, _]) = {}

  /**
   * For any field that requires authorization
   * (marked with `Authorized` field tag)
   *  we would like to prevent field execution
   *  if client have not provided valid OAuth token.
   */
  def beforeField(queryVal: QueryVal, mctx: MiddlewareQueryContext[GraphqlContext, _, _], c: Context[GraphqlContext, _]) = {
    val requiresAuth = c.field.tags.contains(Authorized)

    if (requiresAuth && c.ctx.authToken.isEmpty)
      throw AuthException("Unauthorized access")

    continue
  }
}
