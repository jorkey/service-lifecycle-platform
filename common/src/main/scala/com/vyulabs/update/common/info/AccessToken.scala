package com.vyulabs.update.common.info

import com.vyulabs.update.common.common.Common.{AccountId, ServicesProfileId}
import com.vyulabs.update.common.info.AccountRole.AccountRole
import spray.json.DefaultJsonProtocol

case class AccessToken(account: AccountId, roles: Seq[AccountRole], profile: Option[ServicesProfileId]) {
  def hasRole(role: AccountRole): Boolean = roles.exists(_ == role)
}

object AccessToken extends DefaultJsonProtocol {
  implicit val accessTokenJson = jsonFormat3(AccessToken.apply)
}