package com.vyulabs.update.common.accounts

import com.typesafe.config.Config
import com.vyulabs.update.common.accounts.ServerAccountInfo.{TypeConsumer, TypeService, TypeUser}
import com.vyulabs.update.common.common.Common.AccountId
import com.vyulabs.update.common.info.AccountRole
import spray.json._

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

case class PasswordHash(salt: String, hash: String)

object PasswordHash extends DefaultJsonProtocol {
  implicit val passwordHashJson = jsonFormat2(PasswordHash.apply)

  def apply(password: String): PasswordHash = {
    val salt = generateSalt()
    val hash = generatePasswordHash(password, salt)
    PasswordHash(salt, hash)
  }

  def apply(config: Config): PasswordHash = {
    PasswordHash(config.getString("salt"), config.getString("hash"))
  }

  def generatePasswordHash(password: String, salt: String): String = {
    val saltBytes = Base64.getDecoder().decode(salt)
    val spec = new PBEKeySpec(password.toCharArray, saltBytes, 256, 128)
    val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
    val hash = factory.generateSecret(spec).getEncoded
    Base64.getEncoder().encodeToString(hash)
  }

  private def generateSalt(): String = {
    val random = new SecureRandom()
    val salt = new Array[Byte](16)
    random.nextBytes(salt)
    Base64.getEncoder().encodeToString(salt)
  }
}

case class ServerAccountInfo(`type`: String,
                             account: AccountId, name: String, role: String,
                             passwordHash: Option[PasswordHash],
                             user: Option[UserAccountProperties],
                             consumer: Option[ConsumerAccountProperties]) {
  def toAccountInfo(): AccountInfo = {
    `type` match {
      case TypeUser =>
        UserAccountInfo(account, name, AccountRole.withName(role), user.get)
      case TypeService =>
        ServiceAccountInfo(account, name, AccountRole.withName(role))
      case TypeConsumer =>
        ConsumerAccountInfo(account, name, AccountRole.withName(role), consumer.get)
    }
  }

  def toUserAccountInfo(): UserAccountInfo = {
    assert(`type` == TypeUser)
    UserAccountInfo(account, name, AccountRole.withName(role), user.get)
  }

  def toServiceAccountInfo(): ServiceAccountInfo = {
    assert(`type` == TypeService)
    ServiceAccountInfo(account, name, AccountRole.withName(role))
  }

  def toConsumerAccountInfo(): ConsumerAccountInfo = {
    assert(`type` == TypeConsumer)
    ConsumerAccountInfo(account, name, AccountRole.withName(role), consumer.get)
  }
}

object ServerAccountInfo {
  val TypeUser = "user"
  val TypeService = "service"
  val TypeConsumer = "consumer"
}
