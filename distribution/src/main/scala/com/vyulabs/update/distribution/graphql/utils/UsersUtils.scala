package com.vyulabs.update.distribution.graphql.utils

import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directive1
import akka.http.scaladsl.server.Directives.{complete, onSuccess, optionalHeaderValueByName, provide}
import akka.stream.Materializer
import com.mongodb.client.model.Filters
import com.vyulabs.update.common.common.Common.UserName
import com.vyulabs.update.common.config.DistributionConfig
import com.vyulabs.update.common.info.UserRole.UserRole
import com.vyulabs.update.common.info.{AccessToken, UserInfo, UserRole}
import com.vyulabs.update.distribution.graphql.{AuthenticationException, NotFoundException}
import com.vyulabs.update.distribution.mongo.DatabaseCollections
import com.vyulabs.update.distribution.users.{PasswordHash, ServerUserInfo, UserCredentials}
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

trait UsersUtils extends SprayJsonSupport {
  protected implicit val system: ActorSystem
  protected implicit val materializer: Materializer
  protected implicit val executionContext: ExecutionContext

  protected val config: DistributionConfig
  protected val collections: DatabaseCollections

  def addUser(user: UserName, name: String, password: String,
              roles: Seq[UserRole], email: Option[String], notifications: Option[Seq[String]])(implicit log: Logger): Future[Unit] = {
    log.info(s"Add user ${user} with roles ${roles}")
    for {
      result <- {
        val document = ServerUserInfo(user, name, PasswordHash(password), roles.map(_.toString), email, notifications.getOrElse(Seq.empty))
        collections.Users_Info.insert(document).map(_ => ())
      }
    } yield result
  }

  def removeUser(user: UserName)(implicit log: Logger): Future[Boolean] = {
    log.info(s"Remove user ${user}")
    val filters = Filters.eq("user", user)
    collections.Users_Info.delete(filters).map(_ > 0)
  }

  def changeUser(user: UserName, name: Option[String], oldPassword: Option[String], password: Option[String],
                 roles: Option[Seq[UserRole]], email: Option[String], notifications: Option[Seq[String]])(implicit log: Logger): Future[Boolean] = {
    log.info(s"Change user ${user}")
    val filters = Filters.eq("user", user)
    collections.Users_Info.update(filters, r => r match {
      case Some(r) =>
        for (oldPassword <- oldPassword) {
          if (r.passwordHash.hash != PasswordHash.generatePasswordHash(oldPassword, r.passwordHash.salt)) {
            throw new IOException(s"Password verification error")
          }
        }
        Some(ServerUserInfo(r.user,
          if (name.isDefined) name.get else r.name,
          password.map(PasswordHash(_)).getOrElse(r.passwordHash),
          roles.map(_.map(_.toString)).getOrElse(r.roles),
          if (email.isDefined) (if (email.get.isEmpty) None else email) else r.email,
          notifications.map(_.map(_.toString)).getOrElse(r.notifications),
        ))
      case None =>
        None
    }).map(_ > 0)
  }

  def login(user: UserName, password: String)(implicit log: Logger): Future[AccessToken] = {
    getUserCredentials(user).map {
      case Some(userCredentials) if userCredentials.passwordHash.hash == PasswordHash.generatePasswordHash(password, userCredentials.passwordHash.salt) =>
        AccessToken(user, userCredentials.roles)
      case _ =>
        throw AuthenticationException("Authentication error")
    }
  }

  def whoAmI(user: UserName)(implicit log: Logger): Future[UserInfo] = {
    getUsersInfo(Some(user)).map(_.headOption.getOrElse(throw NotFoundException()))
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
          case authTokenRx(user, password) =>
            onSuccess(login(user, password)).flatMap { token => provide(Some(token)) }
          case _ =>
            throw AuthenticationException("Authentication error")
        }
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

  def getUserCredentials(user: UserName)(implicit log: Logger): Future[Option[UserCredentials]] = {
    val filters = Filters.eq("user", user)
    collections.Users_Info.find(filters).map(_.map(info => UserCredentials(
      info.roles.map(UserRole.withName(_)), info.passwordHash)).headOption)
  }

  def getUsersInfo(user: Option[UserName] = None)(implicit log: Logger): Future[Seq[UserInfo]] = {
    val clientArg = user.map(Filters.eq("user", _))
    val args = clientArg.toSeq
    val filters = if (!args.isEmpty) Filters.and(args.asJava) else new BsonDocument()
    collections.Users_Info.find(filters).map(_.map(info => UserInfo(info.user, info.name,
      info.roles.map(UserRole.withName(_)), info.email, info.notifications)))
  }
}
