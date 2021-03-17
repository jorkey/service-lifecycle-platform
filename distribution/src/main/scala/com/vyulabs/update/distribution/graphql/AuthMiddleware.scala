package com.vyulabs.update.distribution.graphql

import com.vyulabs.update.common.info.UserRole.UserRole
import sangria.execution.{FieldTag, MiddlewareBeforeField, MiddlewareQueryContext}
import sangria.schema.Context

case class Authorized(roles: UserRole*) extends FieldTag

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
    c.field.tags.find(_.isInstanceOf[Authorized]).map(_.asInstanceOf[Authorized]) match {
      case Some(authorized) =>
        c.ctx.accessToken match {
          case Some(token) =>
            if ((authorized.roles.toSet -- token.roles).size == authorized.roles.size) {
              throw AuthException("Unauthorized access")
            }
          case None =>
            throw AuthException("Unauthorized access")
        }
      case None =>
    }
    continue
  }
}
