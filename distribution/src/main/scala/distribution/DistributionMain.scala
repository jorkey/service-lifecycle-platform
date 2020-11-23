package com.vyulabs.update.distribution

import java.io.{File, FileInputStream}

import akka.stream.ActorMaterializer
import akka.actor.ActorSystem
import akka.http.scaladsl.{ConnectionContext, Http, HttpsConnectionContext}
import com.vyulabs.update.common.com.vyulabs.common.utils.Arguments
import com.vyulabs.update.lock.SmartFilesLocker
import distribution.users.UsersCredentials.credentialsFile
import distribution.users.{PasswordHash, UserCredentials, UserRole, UsersCredentials}
import com.vyulabs.update.utils.{IoUtils, Utils}
import distribution.loaders.StateUploader
import org.slf4j.LoggerFactory

import scala.io.StdIn
import distribution.users.UsersCredentials._
import distribution.graphql.{Graphql, GraphqlWorkspace}
import distribution.mongo.{DatabaseCollections, MongoDb}
import java.security.{KeyStore, SecureRandom}

import distribution.Distribution
import distribution.config.{DistributionConfig, SslConfig}
import javax.net.ssl.{KeyManagerFactory, SSLContext, TrustManagerFactory}
import spray.json._

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
    "Arguments: developer\n" +
    "           client\n" +
    "           addUser <userName=value> <role=value>\n" +
    "           removeUser <userName=value>\n" +
    "           changePassword <userName=value>"

  try {
    val command = args(0)
    val arguments = Arguments.parse(args.drop(1))

    implicit val executionContext = ExecutionContext.fromExecutor(null, ex => log.error("Uncatched exception", ex))

    implicit val filesLocker = new SmartFilesLocker()

    val usersCredentials = UsersCredentials()

    command match {
      case "run" =>
        val graphql = new Graphql()

        val config = DistributionConfig.readFromFile().getOrElse {
          Utils.error("No config")
        }

        val mongoDb = new MongoDb("distribution", config.mongoDb)

        val dir = new DistributionDirectory(new File(config.distributionDirectory))
        val collections = new DatabaseCollections(mongoDb, config.instanceState.expireSec)

        config.client.foreach { client =>
          val uploader = StateUploader(config.distributionName, collections, dir, client.uploadStateIntervalSec, client.developerDistributionUrl)
          uploader.setSelfStates(config.instanceId, new File("."), config.developer.map(_.builderDirectory), config.client.map(_.installerDirectory))
          uploader.start()
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
        val userName = arguments.getValue("userName")
        usersCredentials.removeUser(userName)
        if (!IoUtils.writeJsonToFile(credentialsFile, usersCredentials.toJson)) {
          Utils.error("Can't save credentials file")
        }
        sys.exit()

      case "changePassword" =>
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
      Utils.error(ex.getMessage)
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