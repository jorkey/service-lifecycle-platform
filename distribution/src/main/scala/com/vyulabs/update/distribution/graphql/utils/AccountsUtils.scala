package com.vyulabs.update.distribution.graphql.utils

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.Directive1
import akka.http.scaladsl.server.Directives.{complete, onSuccess, optionalHeaderValueByName, provide}
import com.mongodb.client.model.Filters
import com.vyulabs.update.common.common.Common.AccountId
import com.vyulabs.update.common.config.DistributionConfig
import com.vyulabs.update.common.info.AccountRole.AccountRole
import com.vyulabs.update.distribution.graphql.{AuthenticationException, NotFoundException}
import com.vyulabs.update.distribution.mongo.DatabaseCollections
import com.vyulabs.update.common.accounts.{AccountInfo, ConsumerAccountInfo, ConsumerAccountProperties, PasswordHash, ServerAccountInfo, ServiceAccountInfo, UserAccountInfo, UserAccountProperties}
import com.vyulabs.update.common.common.JWT
import org.bson.BsonDocument
import org.slf4j.Logger

import java.io.IOException
import java.util.Base64
import scala.collection.JavaConverters.asJavaIterableConverter
import scala.concurrent.{ExecutionContext, Future}
import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer
import com.vyulabs.update.common.info.{AccessToken, AccountRole}

import java.net.URLDecoder

trait AccountsUtils extends SprayJsonSupport {
  protected implicit val system: ActorSystem
  protected implicit val materializer: Materializer
  protected implicit val executionContext: ExecutionContext

  protected val config: DistributionConfig
  protected val collections: DatabaseCollections

  def addUserAccount(account: AccountId, name: String, role: AccountRole, password: String, info: UserAccountProperties)
                     (implicit log: Logger): Future[Unit] = {
    log.info(s"Add user account ${account} with role ${role}")
    for {
      result <- {
        val document = ServerAccountInfo(ServerAccountInfo.TypeUser,
          account, name, role.toString, Some(PasswordHash(password)), user = Some(info), None)
        collections.Accounts.insert(document).map(_ => ())
      }
    } yield result
  }

  def addServiceAccount(account: AccountId, name: String, role: AccountRole)
                       (implicit log: Logger): Future[Unit] = {
    log.info(s"Add service account ${account} with role ${role}")
    for {
      result <- {
        val document = ServerAccountInfo(ServerAccountInfo.TypeService,
          account, name, role.toString, None, None, None)
        collections.Accounts.insert(document).map(_ => ())
      }
    } yield result
  }

  def addConsumerAccount(account: AccountId, name: String, info: ConsumerAccountProperties)
                        (implicit log: Logger): Future[Unit] = {
    log.info(s"Add consumer account ${account}")
    for {
      result <- {
        val document = ServerAccountInfo(ServerAccountInfo.TypeConsumer,
          account, name, AccountRole.DistributionConsumer.toString, None, None, Some(info))
        collections.Accounts.insert(document).map(_ => ())
      }
    } yield result
  }

  def changeUserAccount(account: AccountId, name: Option[String], role: Option[AccountRole],
                        oldPassword: Option[String], password: Option[String],
                        info: Option[UserAccountProperties])(implicit log: Logger): Future[Boolean] = {
    log.info(s"Change user account ${account}")
    val filters = Filters.eq("account", account)
    collections.Accounts.change(filters, r => {
      for (oldPassword <- oldPassword) {
        if (r.passwordHash.get.hash != PasswordHash.generatePasswordHash(oldPassword, r.passwordHash.get.salt)) {
          throw new IOException(s"Password verification error")
        }
      }
      ServerAccountInfo(ServerAccountInfo.TypeUser,
        r.account,
        if (name.isDefined) name.get else r.name,
        if (role.isDefined) role.get.toString else r.role,
        if (password.isDefined) password.map(PasswordHash(_)) else r.passwordHash,
        if (info.isDefined) info else r.user,
        None)
    }).map(_ > 0)
  }

  def changeServiceAccount(account: AccountId, name: Option[String], role: Option[AccountRole])
                           (implicit log: Logger): Future[Boolean] = {
    log.info(s"Change service account ${account}")
    val filters = Filters.eq("account", account)
    collections.Accounts.change(filters, r => {
      ServerAccountInfo(ServerAccountInfo.TypeService,
        r.account,
        if (name.isDefined) name.get else r.name,
        if (role.isDefined) role.get.toString else r.role,
        None,
        None,
        None)
    }).map(_ > 0)
  }

  def changeConsumerAccount(account: AccountId, name: Option[String],
                            info: Option[ConsumerAccountProperties])(implicit log: Logger): Future[Boolean] = {
    log.info(s"Change user account ${account}")
    val filters = Filters.eq("account", account)
    collections.Accounts.change(filters, r => {
      ServerAccountInfo(ServerAccountInfo.TypeConsumer,
        r.account,
        if (name.isDefined) name.get else r.name,
        AccountRole.DistributionConsumer.toString,
        None,
        None,
        if (info.isDefined) info else r.consumer)
    }).map(_ > 0)
  }

  def removeAccount(account: AccountId)(implicit log: Logger): Future[Boolean] = {
    log.info(s"Remove account ${account}")
    val filters = Filters.eq("account", account)
    collections.Accounts.delete(filters).map(_ > 0)
  }

  def login(account: AccountId, password: String)(implicit log: Logger): Future[AccessToken] = {
    getServerAccountInfo(account).map {
      case Some(ServerAccountInfo(_, _, _, _, Some(passwordHash), _, None)) if passwordHash.hash == PasswordHash.generatePasswordHash(password, passwordHash.salt) =>
        AccessToken(account)
      case _ =>
        throw AuthenticationException("Authentication error")
    }
  }

  def whoAmI(account: AccountId)(implicit log: Logger): Future[UserAccountInfo] = {
    getUserAccountsInfo(Some(account)).map(_.headOption.getOrElse(throw NotFoundException()))
  }

  def encodeAccessToken(token: AccessToken)(implicit log: Logger): String = {
    try {
      JWT.encodeAccessToken(token, config.jwtSecret)
    } catch {
      case ex: Exception =>
        throw AuthenticationException(s"Encode access token error: ${ex.toString}")
    }
  }

  def getAccessToken()(implicit log: Logger): Directive1[AccessToken] = {
    getOptionalAccessToken().flatMap {
      case Some(token) => provide(token)
      case None => complete(StatusCodes.Unauthorized)
    }
  }

  def getAccountInfo()(implicit log: Logger): Directive1[AccountInfo] = {
    getOptionalAccessToken().flatMap {
      case Some(token) =>
        onSuccess(getAccountInfo(token.account)).flatMap {
          case Some(info) => provide(info)
          case None => complete(StatusCodes.Unauthorized)
        }
      case None =>
        complete(StatusCodes.Unauthorized)
    }
  }

  def getAccountRole()(implicit log: Logger): Directive1[AccountRole] = {
    getAccountInfo().map(_.role)
  }

  def getOptionalAccessToken()(implicit log: Logger): Directive1[Option[AccessToken]] = {
    optionalHeaderValueByName("Authorization").flatMap {
      case Some(authorization) =>
        onSuccess(getOptionalAccessTokenFromHeader(authorization)).flatMap {
          token => provide(token)
        }
      case None =>
        optionalCookie("accessToken").flatMap {
          case Some(cookie) =>
            val token = URLDecoder.decode(cookie.value, "utf8")
            provide(JWT.decodeAccessToken(token, config.jwtSecret))
          case None =>
            provide(None)
        }
    }
  }

  def getOptionalAccessTokenFromHeader(authorization: String)(implicit log: Logger): Future[Option[AccessToken]] = {
    val bearerTokenRx = "Bearer (.*)".r
    val basicTokenRx = "Basic (.*)".r
    authorization match {
      case bearerTokenRx(value) =>
        Future(
          try {
            JWT.decodeAccessToken(value, config.jwtSecret)
          } catch {
            case ex: Exception =>
              None
        })
      case basicTokenRx(value) =>
        val authTokenRx = "(.*):(.*)".r
        new String(Base64.getDecoder.decode(value), "utf8") match {
          case authTokenRx(account, password) =>
            login(account, password).map(Some(_))
          case _ =>
            Future.failed(AuthenticationException(s"Invalid authorization ${authorization}"))
        }
      case _ ⇒
        Future.failed(AuthenticationException(s"Invalid authorization ${authorization}"))
    }
  }

  def getServerAccountInfo(account: AccountId)(implicit log: Logger): Future[Option[ServerAccountInfo]] = {
    val filters = Filters.eq("account", account)
    collections.Accounts.find(filters).map(_.headOption)
  }

  def getAccountInfo(account: AccountId)(implicit log: Logger): Future[Option[AccountInfo]] = {
    val filters = Filters.eq("account", account)
    collections.Accounts.find(filters).map(_.headOption.map(_.toAccountInfo()))
  }

  def getUserAccountInfo(account: AccountId)(implicit log: Logger): Future[Option[UserAccountInfo]] = {
    val filters = Filters.and(Filters.eq("account", account), Filters.eq("type", ServerAccountInfo.TypeUser))
    collections.Accounts.find(filters).map(_.headOption.map(_.toUserAccountInfo()))
  }

  def getServiceAccountInfo(account: AccountId)(implicit log: Logger): Future[Option[ServiceAccountInfo]] = {
    val filters = Filters.and(Filters.eq("account", account), Filters.eq("type", ServerAccountInfo.TypeService))
    collections.Accounts.find(filters).map(_.headOption.map(_.toServiceAccountInfo()))
  }

  def getConsumerAccountInfo(account: AccountId)(implicit log: Logger): Future[Option[ConsumerAccountInfo]] = {
    val filters = Filters.and(Filters.eq("account", account), Filters.eq("type", ServerAccountInfo.TypeConsumer))
    collections.Accounts.find(filters).map(_.headOption.map(_.toConsumerAccountInfo()))
  }

  def getAccountsInfo()(implicit log: Logger): Future[Seq[AccountInfo]] = {
    collections.Accounts.find().map(_.map(_.toAccountInfo()))
  }

  def getUserAccountsInfo(account: Option[AccountId] = None)(implicit log: Logger): Future[Seq[UserAccountInfo]] = {
    val args = Seq(Filters.eq("type", ServerAccountInfo.TypeUser)) ++
      account.map(Filters.eq("account", _))
    val filters = if (!args.isEmpty) Filters.and(args.asJava) else new BsonDocument()
    collections.Accounts.find(filters).map(_.map(_.toUserAccountInfo()))
  }

  def getServiceAccountsInfo(account: Option[AccountId] = None)(implicit log: Logger): Future[Seq[ServiceAccountInfo]] = {
    val args = Seq(Filters.eq("type", ServerAccountInfo.TypeService)) ++
      account.map(Filters.eq("account", _))
    val filters = if (!args.isEmpty) Filters.and(args.asJava) else new BsonDocument()
    collections.Accounts.find(filters).map(_.map(_.toServiceAccountInfo()))
  }

  def getConsumerAccountsInfo(account: Option[AccountId] = None)(implicit log: Logger): Future[Seq[ConsumerAccountInfo]] = {
    val args = Seq(Filters.eq("type", ServerAccountInfo.TypeConsumer)) ++
      account.map(Filters.eq("account", _))
    val filters = if (!args.isEmpty) Filters.and(args.asJava) else new BsonDocument()
    collections.Accounts.find(filters).map(_.map(_.toConsumerAccountInfo()))
  }

  def getAccessToken(account: AccountId)(implicit log: Logger): Future[String] = {
    val filter = Filters.eq("account", account)
    collections.Accounts.find(filter).map(accounts => JWT.encodeAccessToken(AccessToken(accounts.headOption.getOrElse(
      throw NotFoundException()).account), config.jwtSecret))
  }
}
