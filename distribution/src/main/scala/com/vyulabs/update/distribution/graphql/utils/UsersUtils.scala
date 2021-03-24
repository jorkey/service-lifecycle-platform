package com.vyulabs.update.distribution.graphql.utils

import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.stream.Materializer
import com.mongodb.client.model.Filters
import com.vyulabs.update.common.common.Common.UserName
import com.vyulabs.update.common.info.UserRole.UserRole
import com.vyulabs.update.common.info.{AccessToken, UserInfo, UserRole}
import com.vyulabs.update.distribution.graphql.AuthenticationException
import com.vyulabs.update.distribution.mongo.DatabaseCollections
import com.vyulabs.update.distribution.users.{PasswordHash, ServerUserInfo, UserCredentials}
import org.bson.BsonDocument
import org.janjaali.sprayjwt.Jwt
import org.janjaali.sprayjwt.algorithms.HS256
import org.slf4j.Logger

import java.io.IOException
import java.math.BigInteger
import java.security.SecureRandom
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directive1
import akka.http.scaladsl.server.Directives.{complete, onSuccess, optionalHeaderValueByName, provide}
import com.vyulabs.update.common.config.DistributionConfig
import org.janjaali.sprayjwt.exceptions.InvalidSignatureException

import scala.collection.JavaConverters.asJavaIterableConverter
import scala.concurrent.{ExecutionContext, Future}
import spray.json._

import java.util.Base64
import scala.util.{Failure, Success}

trait UsersUtils extends SprayJsonSupport {
  protected implicit val system: ActorSystem
  protected implicit val materializer: Materializer
  protected implicit val executionContext: ExecutionContext

  protected val config: DistributionConfig
  protected val collections: DatabaseCollections

  def addUser(userName: UserName, roles: Seq[UserRole], password: String)(implicit log: Logger): Future[Unit] = {
    log.info(s"Add user ${userName} with roles ${roles}")
    for {
      result <- {
        val document = ServerUserInfo(userName, roles.map(_.toString), PasswordHash(password))
        collections.Users_Info.insert(document).map(_ => ())
      }
    } yield result
  }

  def removeUser(userName: UserName)(implicit log: Logger): Future[Boolean] = {
    log.info(s"Remove user ${userName}")
    val filters = Filters.eq("userName", userName)
    collections.Users_Info.delete(filters).map(_ > 0)
  }

  def changeUserPassword(userName: UserName, oldPassword: String, password: String)(implicit log: Logger): Future[Boolean] = {
    for {
      credentials <- getUserCredentials(userName)
      result <- credentials match {
        case Some(credentials) =>
          if (credentials.passwordHash.hash == PasswordHash.generatePasswordHash(oldPassword, credentials.passwordHash.salt)) {
            changeUserPassword(userName, password)
          } else {
            Future.failed(new IOException(s"Password verification error"))
          }
        case None =>
          Future.failed(new IOException(s"Password verification error"))
      }
    } yield result
  }

  def changeUserPassword(userName: UserName, password: String)(implicit log: Logger): Future[Boolean] = {
    log.info(s"Change user ${userName} password")
    val filters = Filters.eq("userName", userName)
    val hash = PasswordHash(password)
    collections.Users_Info.update(filters, r => r match {
      case Some(r) =>
        Some(ServerUserInfo(r.userName, r.roles, hash))
      case None =>
        None
    }).map(_ > 0)
  }

  def login(userName: String, password: String)(implicit log: Logger): Future[AccessToken] = {
    getUserCredentials(userName).map {
      case Some(userCredentials) if userCredentials.passwordHash.hash == PasswordHash.generatePasswordHash(password, userCredentials.passwordHash.salt) =>
        AccessToken(userName, userCredentials.roles)
      case _ =>
        throw AuthenticationException("Authentication error")
    }
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

  def getOptionalAccessToken()(implicit log: Logger): Directive1[Option[AccessToken]] = {
    val bearerTokenRx = "Bearer (.*)".r
    val basicTokenRx = "Basic (.*)".r
    optionalHeaderValueByName("Authorization").flatMap {
      case Some(bearerTokenRx(value)) ⇒
        try {
          Jwt.decode(value, config.jwtSecret) match {
            case Success(jsonValue) ⇒ provide(Some(jsonValue.convertTo[AccessToken]))
            case Failure(_) ⇒ complete(StatusCodes.Unauthorized)
          }
        } catch {
          case _: InvalidSignatureException =>
            complete(StatusCodes.Unauthorized)
        }
      case Some(basicTokenRx(value)) ⇒
        val authTokenRx = "(.*):(.*)".r
        new String(Base64.getDecoder.decode(value), "utf8") match {
          case authTokenRx(userName, password) =>
            onSuccess(login(userName, password)).flatMap { token => provide(Some(token)) }
          case _ =>
            throw AuthenticationException("Authentication error")
        }
      case Some(_) ⇒
        complete(StatusCodes.Unauthorized)
      case _ ⇒
        provide(None)
    }
  }

  def getAccessToken()(implicit log: Logger): Directive1[AccessToken] = {
    getOptionalAccessToken().flatMap {
      case Some(token) => provide(token)
      case None => complete(StatusCodes.Unauthorized)
    }
  }

  def getUserCredentials(userName: UserName)(implicit log: Logger): Future[Option[UserCredentials]] = {
    val filters = Filters.eq("userName", userName)
    collections.Users_Info.find(filters).map(_.map(info => UserCredentials(
      info.roles.map(UserRole.withName(_)), info.passwordHash)).headOption)
  }

  def getUsersInfo(userName: Option[UserName] = None)(implicit log: Logger): Future[Seq[UserInfo]] = {
    val clientArg = userName.map(Filters.eq("userName", _))
    val args = clientArg.toSeq
    val filters = if (!args.isEmpty) Filters.and(args.asJava) else new BsonDocument()
    collections.Users_Info.find(filters).map(_.map(info => UserInfo(info.userName,
      info.roles.map(UserRole.withName(_)))))
  }
}
