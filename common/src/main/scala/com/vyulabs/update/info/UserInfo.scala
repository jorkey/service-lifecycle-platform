package com.vyulabs.update.info

import com.vyulabs.update.common.Common.UserName
import com.vyulabs.update.info.UserRole.UserRole
import spray.json.{DefaultJsonProtocol}

case class UserInfo(name: UserName, role: UserRole)

object UserInfo extends DefaultJsonProtocol {
  import UserRole._

  implicit val userInfoJson = jsonFormat2(UserInfo.apply)
}
