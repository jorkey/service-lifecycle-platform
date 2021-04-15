package com.vyulabs.update.common.info

import com.vyulabs.update.common.common.Common.UserId
import com.vyulabs.update.common.info.UserRole.UserRole
import spray.json.DefaultJsonProtocol

case class UserInfo(user: UserId, human: Boolean, name: String,
                    roles: Seq[UserRole], email: Option[String], notifications: Seq[String])

object UserInfo extends DefaultJsonProtocol {
  implicit val userInfoJson = jsonFormat6(UserInfo.apply)
}
