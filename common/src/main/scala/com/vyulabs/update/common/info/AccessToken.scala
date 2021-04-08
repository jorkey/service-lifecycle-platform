package com.vyulabs.update.common.info

import com.vyulabs.update.common.common.Common.UserName
import com.vyulabs.update.common.info.UserRole.UserRole
import com.vyulabs.update.common.info.UserRole.UserRole
import spray.json.DefaultJsonProtocol

case class AccessToken(user: UserName, roles: Seq[UserRole]) {
  def hasRole(role: UserRole): Boolean = roles.exists(_ == role)
}

object AccessToken extends DefaultJsonProtocol {
  implicit val accessTokenJson = jsonFormat2(AccessToken.apply)
}