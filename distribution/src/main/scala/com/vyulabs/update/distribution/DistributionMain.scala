package com.vyulabs.update.distribution

import java.io.{File, FileInputStream}
import akka.stream.ActorMaterializer
import akka.actor.ActorSystem
import akka.http.scaladsl.{ConnectionContext, Http, HttpsConnectionContext}
import com.vyulabs.update.common.common.Common
import com.vyulabs.update.common.common.Arguments
import com.vyulabs.update.common.distribution.server.DistributionDirectory
import com.vyulabs.update.distribution.config.{DistributionConfig, SslConfig}
import com.vyulabs.update.distribution.graphql.{Graphql, GraphqlWorkspace}
import com.vyulabs.update.distribution.loaders.StateUploader
import com.vyulabs.update.distribution.logger.LogStorer
import com.vyulabs.update.distribution.mongo.{DatabaseCollections, MongoDb}
import com.vyulabs.update.common.lock.SmartFilesLocker
import com.vyulabs.update.distribution.users.UsersCredentials.credentialsFile
import com.vyulabs.update.common.utils.{IoUtils, Utils}
import org.slf4j.LoggerFactory

import scala.io.StdIn
import com.vyulabs.update.distribution.users.UsersCredentials._

import java.security.{KeyStore, SecureRandom}
import com.vyulabs.update.distribution.users.{PasswordHash, UserCredentials, UsersCredentials}
import com.vyulabs.update.common.info.UserRole
import com.vyulabs.update.common.logger.{LogBuffer, LogSender, TraceAppender}

import javax.net.ssl.{KeyManagerFactory, SSLContext, TrustManagerFactory}
import spray.json._

import java.util.{Timer, TimerTask}
import scala.concurrent.{Await, ExecutionContext, Future}

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 19.04.19.
  * Copyright FanDate, Inc.
  */
object DistributionMain extends App {
  implicit val system = ActorSystem("Distribution")
  implicit val materializer = ActorMaterializer()

  implicit val log = LoggerFactory.getLogger(this.getClass)

  if (args.size < 1) {
    Utils.error(usage())
  }

  def usage() =
    "Arguments: run\n" +
    "           addUser <userName=value> <role=value>\n" +
    "           removeUser <userName=value>\n" +
    "           changePassword <userName=value>"

  try {
    val command = args(0)

    implicit val executionContext = ExecutionContext.fromExecutor(null, ex => log.error("Uncatched exception", ex))

    implicit val filesLocker = new SmartFilesLocker()

    val usersCredentials = UsersCredentials()

    command match {
      case "run" =>
        val graphql = new Graphql()

        val config = DistributionConfig.readFromFile().getOrElse {
          Utils.error("No config")
        }

        val mongoDb = new MongoDb(config.mongoDbConnection, config.mongoDbName)

        val dir = new DistributionDirectory(new File(config.distributionDirectory))
        val collections = new DatabaseCollections(mongoDb, config.instanceState.expireSec)

        TraceAppender.handleLogs(new LogStorer(config.distributionName, Common.DistributionServiceName, config.instanceId, collections))

        config.uploadStateConfigs.getOrElse(Seq.empty).foreach { uploadConfig =>
          StateUploader(config.distributionName, collections, dir, uploadConfig.uploadStateIntervalSec, uploadConfig.distributionUrl).start()
        }

        val selfUpdater = new SelfUpdater(collections)
        selfUpdater.start()

        val workspace = GraphqlWorkspace(config.distributionName, config.versionHistory, config.faultReportsConfig, collections, dir)
        val distribution = new Distribution(workspace, usersCredentials, graphql)

        var server = Http().newServerAt("0.0.0.0", config.network.port)
        config.network.ssl.foreach {
          log.info("Enable https")
          ssl => server = server.enableHttps(makeHttpsContext(ssl))
        }
        server.bind(distribution.route)

      case "addUser" =>
        val arguments = Arguments.parse(args.drop(1), Set("userName"))
        val userName = arguments.getValue("userName")
        val role = UserRole.withName(arguments.getValue("role"))
        val password = StdIn.readLine("Enter password: ")
        if (usersCredentials.getCredentials(userName).isDefined) {
          Utils.error(s"User ${userName} credentials already exists")
        }
        usersCredentials.addUser(userName, UserCredentials(role, PasswordHash(password)))
        if (!IoUtils.writeJsonToFile(credentialsFile, usersCredentials.toJson)) {
          Utils.error("Can't save credentials file")
        }
        sys.exit()

      case "removeUser" =>
        val arguments = Arguments.parse(args.drop(1), Set("userName"))
        val userName = arguments.getValue("userName")
        usersCredentials.removeUser(userName)
        if (!IoUtils.writeJsonToFile(credentialsFile, usersCredentials.toJson)) {
          Utils.error("Can't save credentials file")
        }
        sys.exit()

      case "changePassword" =>
        val arguments = Arguments.parse(args.drop(1), Set("userName"))
        val userName = arguments.getValue("userName")
        val password = StdIn.readLine("Enter password: ")
        usersCredentials.getCredentials(userName) match {
          case Some(credentials) =>
            credentials.password = PasswordHash(password)
            if (!IoUtils.writeJsonToFile(credentialsFile, usersCredentials.toJson)) {
              Utils.error("Can't save credentials file")
            }
          case None =>
            Utils.error(s"No user ${userName} credentials")
        }
        sys.exit()

      case _ =>
        Utils.error(s"Invalid command ${command}\n${usage()}")
    }
  } catch {
    case ex: Throwable =>
      log.error("Exception", ex)
      Utils.error(ex.toString)
  }

  def makeHttpsContext(config: SslConfig): HttpsConnectionContext = {
    val keyStore = KeyStore.getInstance("PKCS12")
    val keyStoreStream = new FileInputStream(new File(config.keyStoreFile))

    keyStore.load(keyStoreStream, config.keyStorePassword.toCharArray)

    val keyManagerFactory = KeyManagerFactory.getInstance("SunX509")
    keyManagerFactory.init(keyStore, config.keyStorePassword.toCharArray)

    val trustManagerFactory = TrustManagerFactory.getInstance("SunX509")
    trustManagerFactory.init(keyStore)

    val sslContext = SSLContext.getInstance("TLS")
    sslContext.init(keyManagerFactory.getKeyManagers, trustManagerFactory.getTrustManagers, new SecureRandom)
    ConnectionContext.httpsServer(sslContext)
  }
}