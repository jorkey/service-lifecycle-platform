package com.vyulabs.update.distribution.graphql.utils

import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directive1
import akka.http.scaladsl.server.Directives.{complete, onSuccess, optionalHeaderValueByName, provide}
import akka.stream.Materializer
import com.mongodb.client.model.Filters
import com.vyulabs.update.common.common.Common.{AccountId, ServicesProfileId}
import com.vyulabs.update.common.config.DistributionConfig
import com.vyulabs.update.common.info.AccountRole.AccountRole
import com.vyulabs.update.common.info.{AccessToken, AccountInfo, AccountRole, ConsumerInfo, HumanInfo}
import com.vyulabs.update.distribution.graphql.{AuthenticationException, NotFoundException}
import com.vyulabs.update.distribution.mongo.DatabaseCollections
import com.vyulabs.update.distribution.accounts.{AccountCredentials, PasswordHash, ServerAccountInfo}
import org.bson.BsonDocument
import org.janjaali.sprayjwt.Jwt
import org.janjaali.sprayjwt.algorithms.HS256
import org.janjaali.sprayjwt.exceptions.InvalidSignatureException
import org.slf4j.Logger
import spray.json._

import java.io.IOException
import java.util.Base64
import scala.collection.JavaConverters.asJavaIterableConverter
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

trait AccountsUtils extends SprayJsonSupport {
  protected implicit val system: ActorSystem
  protected implicit val materializer: Materializer
  protected implicit val executionContext: ExecutionContext

  protected val config: DistributionConfig
  protected val collections: DatabaseCollections

  def addAccount(account: AccountId, name: String, password: String,
                 roles: Seq[AccountRole], human: Option[HumanInfo], consumer: Option[ConsumerInfo])
                (implicit log: Logger): Future[Unit] = {
    log.info(s"Add account ${account} with roles ${roles}")
    for {
      result <- {
        val document = ServerAccountInfo(account, name, PasswordHash(password), roles.map(_.toString), human, consumer)
        collections.Accounts.insert(document).map(_ => ())
      }
    } yield result
  }

  def removeAccount(account: AccountId)(implicit log: Logger): Future[Boolean] = {
    log.info(s"Remove account ${account}")
    val filters = Filters.eq("account", account)
    collections.Accounts.delete(filters).map(_ > 0)
  }

  def changeAccount(account: AccountId, name: Option[String], oldPassword: Option[String], password: Option[String],
                    roles: Option[Seq[AccountRole]], profiles: Option[ServicesProfileId],
                    email: Option[String], notifications: Option[Seq[String]])(implicit log: Logger): Future[Boolean] = {
    log.info(s"Change account ${account}")
    val filters = Filters.eq("account", account)
    collections.Accounts.change(filters, r => {
      for (oldPassword <- oldPassword) {
        if (r.passwordHash.hash != PasswordHash.generatePasswordHash(oldPassword, r.passwordHash.salt)) {
          throw new IOException(s"Password verification error")
        }
      }
      ServerAccountInfo(r.account,
        if (name.isDefined) name.get else r.name,
        password.map(PasswordHash(_)).getOrElse(r.passwordHash),
        roles.map(_.map(_.toString)).getOrElse(r.roles),
        r.human, r.consumer)
    }).map(_ > 0)
  }

  def login(account: AccountId, password: String)(implicit log: Logger): Future[AccessToken] = {
    getAccountCredentials(account).map {
      case Some(accountCredentials) if accountCredentials.passwordHash.hash == PasswordHash.generatePasswordHash(password, accountCredentials.passwordHash.salt) =>
        AccessToken(account, accountCredentials.roles, accountCredentials.profile)
      case _ =>
        throw AuthenticationException("Authentication error")
    }
  }

  def whoAmI(account: AccountId)(implicit log: Logger): Future[AccountInfo] = {
    getAccountsInfo(Some(account)).map(_.headOption.getOrElse(throw NotFoundException()))
  }

  def encodeAccessToken(token: AccessToken)(implicit log: Logger): String = {
    Jwt.encode(token.toJson, config.jwtSecret, HS256) match {
      case Success(value) =>
        value
      case Failure(ex) =>
        log.error("Jwt encoding error", ex)
        throw AuthenticationException(s"Authentication error: ${ex.toString}")
    }
  }

  def getAccessToken()(implicit log: Logger): Directive1[AccessToken] = {
    getOptionalAccessToken().flatMap {
      case Some(token) => provide(token)
      case None => complete(StatusCodes.Unauthorized)
    }
  }

  def getOptionalAccessToken()(implicit log: Logger): Directive1[Option[AccessToken]] = {
    optionalHeaderValueByName("Authorization").flatMap {
      case Some(authorization) =>
        onSuccess(getOptionalAccessToken(authorization)).flatMap { token => provide(token) }
      case None =>
        provide(None)
    }
  }

  def getOptionalAccessToken(authorization: String)(implicit log: Logger): Future[Option[AccessToken]] = {
    val bearerTokenRx = "Bearer (.*)".r
    val basicTokenRx = "Basic (.*)".r
    authorization match {
      case bearerTokenRx(value) =>
        try {
          Jwt.decode(value, config.jwtSecret) match {
            case Success(jsonValue) ⇒ Future(Some(jsonValue.convertTo[AccessToken]))
            case Failure(_) ⇒ Future(None)
          }
        } catch {
          case _: InvalidSignatureException =>
            Future(None)
        }
      case basicTokenRx(value) ⇒
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

  def getAccountCredentials(account: AccountId)(implicit log: Logger): Future[Option[AccountCredentials]] = {
    val filters = Filters.eq("account", account)
    collections.Accounts.find(filters).map(_.map(info => AccountCredentials(
      info.roles.map(AccountRole.withName(_)), info.consumer.map(_.profile), info.passwordHash)).headOption)
  }

  def getAccountsInfo(account: Option[AccountId] = None)(implicit log: Logger): Future[Seq[AccountInfo]] = {
    val accountArg = account.map(Filters.eq("account", _))
    val args = accountArg.toSeq
    val filters = if (!args.isEmpty) Filters.and(args.asJava) else new BsonDocument()
    collections.Accounts.find(filters).map(_.map(info => AccountInfo(info.account,
      info.name, info.roles.map(AccountRole.withName(_)), info.human, info.consumer)))
  }

  def getAccountInfo(account: AccountId)(implicit log: Logger): Future[Option[AccountInfo]] = {
    getAccountsInfo(Some(account)).map(_.headOption)
  }
}
