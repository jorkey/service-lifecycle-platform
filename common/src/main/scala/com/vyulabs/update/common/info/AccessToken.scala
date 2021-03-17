package com.vyulabs.update.common.info

import com.vyulabs.update.common.common.Common.UserName
import com.vyulabs.update.common.info.UserRole.UserRole
import spray.json.DefaultJsonProtocol

case class AccessToken(userName: UserName, role: UserRole)

object AccessToken extends DefaultJsonProtocol {
  implicit val accessTokenJson = jsonFormat2(AccessToken.apply)
}