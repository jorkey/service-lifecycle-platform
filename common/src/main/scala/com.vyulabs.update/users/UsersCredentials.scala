package com.vyulabs.update.users

import java.io.File
import java.security.SecureRandom
import java.util.Base64

import com.typesafe.config.Config
import com.vyulabs.update.common.Common.UserName
import com.vyulabs.update.users.UserRole.UserRole
import com.vyulabs.update.utils.{IoUtils, Utils}
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import org.slf4j.{Logger, LoggerFactory}
import spray.json._

object UserRole extends Enumeration with DefaultJsonProtocol {
  type UserRole = Value
  val None, Administrator, Client, Service = Value

  implicit object UserRoleJsonFormat extends RootJsonFormat[UserRole] {
    def write(value: UserRole) = JsString(value.toString)
    def read(value: JsValue) = withName(value.asInstanceOf[JsString].value)
  }
}

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

case class UserCredentials(role: UserRole, var password: PasswordHash)

object UserCredentials extends DefaultJsonProtocol {
  import UserRole._
  import PasswordHash._

  implicit val userCredentialsJson = jsonFormat2(UserCredentials.apply)
}

case class UsersCredentials(var credentials: Map[UserName, UserCredentials]) {
  def addUser(user: UserName, credentials: UserCredentials): Unit = {
    this.credentials += (user -> credentials)
  }

  def getCredentials(user: UserName): Option[UserCredentials] = {
    credentials.get(user)
  }

  def removeUser(user: UserName): Unit = {
    credentials -= user
  }
}

object UsersCredentials extends DefaultJsonProtocol {
  import UserCredentials._

  implicit val usersCredentialsJson = jsonFormat1(UsersCredentials.apply)

  val credentialsFile = new File("credentials.json")

  def apply(): UsersCredentials = {
    implicit val log = LoggerFactory.getLogger(this.getClass)

    if (credentialsFile.exists()) {
      log.debug(s"Parse ${credentialsFile}")
      IoUtils.readFileToJson[UsersCredentials](credentialsFile).getOrElse {
        Utils.error(s"Can't read ${credentialsFile}")
      }
    } else {
      log.info(s"File ${credentialsFile} not exists")
      new UsersCredentials(Map.empty)
    }
  }
}

case class UserInfo(name: UserName, role: UserRole)

object UserInfo extends DefaultJsonProtocol {
  import UserRole._

  implicit val userInfoJson = jsonFormat2(UserInfo.apply)
}
