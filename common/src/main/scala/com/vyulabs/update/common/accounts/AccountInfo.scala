package com.vyulabs.update.common.accounts

import com.vyulabs.update.common.common.Common.{AccountId, ServicesProfileId}
import com.vyulabs.update.common.info.AccountRole.AccountRole
import spray.json.DefaultJsonProtocol

trait AccountInfo {
  val account: AccountId
  val name: String
  val role: AccountRole
}

case class UserAccountProperties(email: Option[String], notifications: Seq[String])

object UserAccountProperties extends DefaultJsonProtocol {
  implicit val accountInfoJson = jsonFormat2(UserAccountProperties.apply)
}

case class UserAccountInfo(account: AccountId, name: String, role: AccountRole, properties: UserAccountProperties) extends AccountInfo

object UserAccountInfo extends DefaultJsonProtocol {
  implicit val accountInfoJson = jsonFormat4(UserAccountInfo.apply)
}

case class ConsumerAccountProperties(profile: ServicesProfileId, url: String)

object ConsumerAccountProperties extends DefaultJsonProtocol {
  implicit val accountInfoJson = jsonFormat2(ConsumerAccountProperties.apply)
}

case class ConsumerAccountInfo(account: AccountId, name: String, role: AccountRole, properties: ConsumerAccountProperties) extends AccountInfo

case class ServiceAccountInfo(account: AccountId, name: String, role: AccountRole) extends AccountInfo
