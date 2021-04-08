package com.vyulabs.update.common.info

import com.vyulabs.update.common.common.Common.UserName
import com.vyulabs.update.common.info.UserRole.UserRole
import com.vyulabs.update.common.info.UserRole.UserRole
import spray.json.DefaultJsonProtocol

case class HumanInfo(firstName: String, lastName: String, email: String)

object HumanInfo extends DefaultJsonProtocol {
  implicit val humanInfoJson = jsonFormat3(HumanInfo.apply)
}

case class UserInfo(user: UserName, roles: Seq[UserRole], human: Option[HumanInfo])

object UserInfo extends DefaultJsonProtocol {
  implicit val userInfoJson = jsonFormat3(UserInfo.apply)
}
