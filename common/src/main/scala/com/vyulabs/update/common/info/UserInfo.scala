package com.vyulabs.update.common.info

import com.vyulabs.update.common.common.Common.UserName
import com.vyulabs.update.common.info.UserRole.UserRole
import com.vyulabs.update.common.info.UserRole.UserRole
import spray.json.DefaultJsonProtocol

case class UserInfo(name: UserName, roles: Seq[UserRole])

object UserInfo extends DefaultJsonProtocol {
  implicit val userInfoJson = jsonFormat2(UserInfo.apply)
}
