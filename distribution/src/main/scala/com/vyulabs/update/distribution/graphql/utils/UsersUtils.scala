package com.vyulabs.update.distribution.graphql.utils

import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.stream.Materializer
import com.mongodb.client.model.Filters
import com.vyulabs.update.common.common.Common.UserName
import com.vyulabs.update.common.info.{UserInfo, UserRole}
import com.vyulabs.update.common.info.UserRole.UserRole
import com.vyulabs.update.distribution.mongo.{DatabaseCollections, UserInfoDocument}
import com.vyulabs.update.distribution.users.{PasswordHash, ServerUserInfo, UserCredentials}
import org.bson.BsonDocument
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters.asJavaIterableConverter
import scala.concurrent.{ExecutionContext, Future}

trait UsersUtils extends SprayJsonSupport {
  private implicit val log = LoggerFactory.getLogger(this.getClass)

  protected implicit val system: ActorSystem
  protected implicit val materializer: Materializer
  protected implicit val executionContext: ExecutionContext

  protected val collections: DatabaseCollections

  def addUser(userName: UserName, role: UserRole, password: String): Unit = {
    log.info(s"Add user ${userName} with role ${role}")
    for {
      collection <- collections.Users_Info
      result <- {
        val document = UserInfoDocument(ServerUserInfo(userName, role.toString, PasswordHash(password)))
        collection.insert(document).map(_ => true)
      }
    } yield result
  }

  def removeUser(userName: UserName): Future[Boolean] = {
    log.info(s"Remove user ${userName}")
    val filters = Filters.eq("info.userName", userName)
    for {
      collection <- collections.Users_Info
      profile <- {
        collection.delete(filters).map(_.getDeletedCount > 0)
      }
    } yield profile
  }

  def getUserCredentials(userName: UserName): Future[Option[UserCredentials]] = {
    val filters = Filters.eq("info.userName", userName)
    for {
      collection <- collections.Users_Info
      info <- collection.find(filters).map(_.map(info => UserCredentials(
        UserRole.withName(info.info.role), info.info.password)).headOption)
    } yield info
  }

  def getUsersInfo(userName: Option[UserName] = None): Future[Seq[UserInfo]] = {
    val clientArg = userName.map(Filters.eq("info.userName", _))
    val args = clientArg.toSeq
    val filters = if (!args.isEmpty) Filters.and(args.asJava) else new BsonDocument()
    for {
      collection <- collections.Users_Info
      info <- collection.find(filters).map(_.map(info => UserInfo(info.info.userName, UserRole.withName(info.info.role))))
    } yield info
  }
}
