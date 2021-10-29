package com.vyulabs.update.distribution.graphql

import com.vyulabs.update.common.info.AccountRole.AccountRole
import sangria.execution.{FieldTag, MiddlewareBeforeField, MiddlewareQueryContext}
import sangria.schema.Context

case class Authorized(roles: AccountRole*) extends FieldTag

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
        c.ctx.accountInfo match {
          case Some(accountInfo) =>
            if ((authorized.roles.toSet - accountInfo.role).size == authorized.roles.size) {
              throw AuthException(s"Unauthorized access: query ${mctx.queryAst.toString}, authorized ${authorized}, account info ${accountInfo}")
            }
          case None =>
            throw AuthException("Unauthorized access. No or invalid access token.")
        }
      case None =>
    }
    continue
  }
}
