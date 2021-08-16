package com.vyulabs.update.common.info

import com.vyulabs.update.common.common.Common.{AccountId}
import spray.json.DefaultJsonProtocol

case class AccessToken(account: AccountId)

object AccessToken extends DefaultJsonProtocol {
  implicit val accessTokenJson = jsonFormat1(AccessToken.apply)
}