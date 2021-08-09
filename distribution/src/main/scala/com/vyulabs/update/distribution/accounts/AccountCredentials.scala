package com.vyulabs.update.distribution.accounts

import com.typesafe.config.Config
import com.vyulabs.update.common.common.Common.{AccountId, ServicesProfileId}
import com.vyulabs.update.common.info.AccountRole.AccountRole
import com.vyulabs.update.common.info.{ConsumerInfo, HumanInfo}
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

case class AccountCredentials(roles: Seq[AccountRole], profile: Option[ServicesProfileId], var passwordHash: PasswordHash)

// We can't use enumeration role because mongodb does not have codec for enumerations.
case class ServerAccountInfo(account: AccountId, name: String, passwordHash: PasswordHash,
                             roles: Seq[String], human: Option[HumanInfo], consumer: Option[ConsumerInfo])
