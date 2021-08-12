package com.vyulabs.update.common.info

import com.vyulabs.update.common.common.Common.{ServicesProfileId, AccountId}
import com.vyulabs.update.common.info.AccountRole.AccountRole
import spray.json.DefaultJsonProtocol

case class HumanInfo(email: Option[String], notifications: Seq[String])

object HumanInfo extends DefaultJsonProtocol {
  implicit val accountInfoJson = jsonFormat2(HumanInfo.apply)
}

case class ConsumerInfo(profile: ServicesProfileId, url: String)

object ConsumerInfo extends DefaultJsonProtocol {
  implicit val accountInfoJson = jsonFormat2(ConsumerInfo.apply)
}

case class AccountInfo(account: AccountId, name: String, roles: Seq[AccountRole],
                       human: Option[HumanInfo], consumer: Option[ConsumerInfo])

object AccountInfo extends DefaultJsonProtocol {
  implicit val accountInfoJson = jsonFormat5(AccountInfo.apply)
}
