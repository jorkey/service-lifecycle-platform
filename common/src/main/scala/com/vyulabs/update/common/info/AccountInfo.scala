package com.vyulabs.update.common.info

import com.vyulabs.update.common.common.Common.{ServicesProfileId, AccountId}
import com.vyulabs.update.common.info.AccountRole.AccountRole
import spray.json.DefaultJsonProtocol

case class AccountInfo(account: AccountId, name: String,
                       roles: Seq[AccountRole], profile: Option[ServicesProfileId], email: Option[String], notifications: Seq[String])

object AccountInfo extends DefaultJsonProtocol {
  implicit val accountInfoJson = jsonFormat6(AccountInfo.apply)
}
