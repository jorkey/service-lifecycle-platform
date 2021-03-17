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
import scala.collection.JavaConverters.asJavaIterableConverter
import scala.concurrent.{ExecutionContext, Future}
import spray.json._

import scala.util.{Failure, Success}

trait UsersUtils extends SprayJsonSupport {
  protected implicit val system: ActorSystem
  protected implicit val materializer: Materializer
  protected implicit val executionContext: ExecutionContext

  protected val collections: DatabaseCollections

  def addUser(userName: UserName, role: UserRole, password: String)(implicit log: Logger): Future[Unit] = {
    log.info(s"Add user ${userName} with role ${role}")
    for {
      result <- {
        val document = ServerUserInfo(userName, role.toString, PasswordHash(password))
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
        Some(ServerUserInfo(r.userName, r.role, hash))
      case None =>
        None
    }).map(_ > 0)
  }

  def login(userName: String, password: String)(implicit log: Logger): Future[String] = {
    getUserCredentials(userName).map {
      case Some(userCredentials) if userCredentials.passwordHash.hash == PasswordHash.generatePasswordHash(password, userCredentials.passwordHash.salt) =>
        val token = AccessToken(userName, userCredentials.role).toJson
        Jwt.encode(token, "secret", HS256) match {
          case Success(value) =>
            value
          case Failure(ex) =>
            log.error("Jwt encoding error", ex)
            throw AuthenticationException(s"Authentication error: ${ex.toString}")
        }
      case _ =>
        throw AuthenticationException("Authentication error")
    }
  }

  def getUserCredentials(userName: UserName)(implicit log: Logger): Future[Option[UserCredentials]] = {
    val filters = Filters.eq("userName", userName)
    collections.Users_Info.find(filters).map(_.map(info => UserCredentials(
      UserRole.withName(info.role), info.passwordHash)).headOption)
  }

  def getUsersInfo(userName: Option[UserName] = None)(implicit log: Logger): Future[Seq[UserInfo]] = {
    val clientArg = userName.map(Filters.eq("userName", _))
    val args = clientArg.toSeq
    val filters = if (!args.isEmpty) Filters.and(args.asJava) else new BsonDocument()
    collections.Users_Info.find(filters).map(_.map(info => UserInfo(info.userName, UserRole.withName(info.role))))
  }
}
