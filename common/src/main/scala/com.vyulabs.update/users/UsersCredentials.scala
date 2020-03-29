package com.vyulabs.update.users

import java.io.File
import java.security.SecureRandom
import java.util.Base64

import com.typesafe.config.{Config, ConfigFactory, ConfigValueFactory}
import com.vyulabs.update.common.Common.UserName
import com.vyulabs.update.users.UserRole.UserRole
import com.vyulabs.update.utils.UpdateUtils
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.JavaConverters._

object UserRole extends Enumeration {
  type UserRole = Value
  val None, Administrator, Client, Service = Value
}

case class PasswordHash(salt: String, hash: String) {
  def toConfig(): Config = {
    ConfigFactory.empty()
      .withValue("salt", ConfigValueFactory.fromAnyRef(salt))
      .withValue("hash", ConfigValueFactory.fromAnyRef(hash))
  }
}

object PasswordHash {
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

case class UserCredentials(role: UserRole, var passwordHash: PasswordHash) {
  def toConfig(config: Config): Config = {
    config
      .withValue("role", ConfigValueFactory.fromAnyRef(role.toString))
      .withValue("password", ConfigValueFactory.fromAnyRef(passwordHash.toConfig().root()))
  }
}

object UserCredentials {
  def apply(config: Config): UserCredentials = {
    new UserCredentials(UserRole.withName(config.getString("role")), PasswordHash(config.getConfig("password")))
  }
}

class UsersCredentials(private var users: Map[UserName, UserCredentials]) {
  implicit val log = LoggerFactory.getLogger(this.getClass)

  def addUser(user: UserName, credentials: UserCredentials): Unit = {
    users += (user -> credentials)
  }

  def getCredentials(user: UserName): Option[UserCredentials] = {
    users.get(user)
  }

  def removeUser(user: UserName): Unit = {
    users -= user
  }

  def getRole(user: UserName): UserRole = {
    users.get(user).map(_.role).getOrElse(UserRole.None)
  }

  def save(): Boolean = {
    UpdateUtils.writeConfigFile(UsersCredentials.credentialsFile, toConfig())
  }

  private def toConfig(): Config = {
    ConfigFactory.empty()
      .withValue("credentials", ConfigValueFactory.fromIterable(
        users.map(account => {
          account._2.toConfig(ConfigFactory.empty()
            .withValue("user", ConfigValueFactory.fromAnyRef(account._1))).root()
        }).asJava))
  }
}

object UsersCredentials {
  val credentialsFile = new File("credentials.json")

  def apply(): UsersCredentials = {
    implicit val log = LoggerFactory.getLogger(this.getClass)

    if (credentialsFile.exists()) {
      log.debug(s"Parse ${credentialsFile}")
      val config = UpdateUtils.parseConfigFile(credentialsFile).getOrElse {
        sys.error(s"Can't read ${credentialsFile}")
      }
      val users = config.getConfigList("credentials").asScala.foldLeft(Map.empty[UserName, UserCredentials]) {
        case (map, config) => map + (config.getString("user") -> UserCredentials(config))
      }
      new UsersCredentials(users)
    } else {
      log.info(s"File ${credentialsFile} not exists")
      new UsersCredentials(Map.empty)
    }
  }
}