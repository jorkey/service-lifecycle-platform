package com.vyulabs.update.distribution

import akka.actor.ActorSystem
import akka.http.scaladsl.{ConnectionContext, Http, HttpsConnectionContext}
import akka.stream.ActorMaterializer
import com.vyulabs.update.common.common.Common
import com.vyulabs.update.common.config.{DistributionConfig, SslConfig}
import com.vyulabs.update.common.distribution.server.DistributionDirectory
import com.vyulabs.update.common.lock.SmartFilesLocker
import com.vyulabs.update.common.logger.TraceAppender
import com.vyulabs.update.common.utils.Utils
import com.vyulabs.update.distribution.common.AkkaTimer
import com.vyulabs.update.distribution.graphql.{Graphql, GraphqlWorkspace}
import com.vyulabs.update.distribution.loaders.StateUploader
import com.vyulabs.update.distribution.logger.LogStorekeeper
import com.vyulabs.update.distribution.mongo.{DatabaseCollections, MongoDb}
import com.vyulabs.update.distribution.task.TaskManager
import com.vyulabs.update.distribution.updater.AutoUpdater
import org.slf4j.LoggerFactory

import java.io.{File, FileInputStream}
import java.security.{KeyStore, SecureRandom}
import javax.net.ssl.{KeyManagerFactory, SSLContext, TrustManagerFactory}
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 19.04.19.
  * Copyright FanDate, Inc.
  */
object DistributionMain extends App {
  implicit val system = ActorSystem("Distribution")
  implicit val materializer = ActorMaterializer()

  implicit val log = LoggerFactory.getLogger(this.getClass)

  try {
    implicit val executionContext = ExecutionContext.fromExecutor(null, ex => log.error("Uncatched exception", ex))
    implicit val timer = new AkkaTimer(system.scheduler)
    implicit val filesLocker = new SmartFilesLocker()

    val graphql = new Graphql()

    val config = DistributionConfig.readFromFile().getOrElse {
      Utils.error("No config")
    }

    val db = new MongoDb(config.mongoDb.name, config.mongoDb.connection, config.mongoDb.temporary.getOrElse(false))
    val logsDb = config.logsMongoDb match {
      case Some(logsDb) =>
        new MongoDb(logsDb.name, logsDb.connection, logsDb.temporary.getOrElse(false))
      case None =>
        db
    }

    val collections = new DatabaseCollections(
      db, logsDb,
      config.serviceStates.expirationTimeout,
      config.logs.taskLogExpirationTimeout,
     true)
    val dir = new DistributionDirectory(new File("."))
    val taskManager = new TaskManager(task => new LogStorekeeper(Common.DistributionServiceName, Some(task),
      config.instance, collections.Log_Lines, config.logs))

    Await.result(collections.init(), Duration.Inf)

    TraceAppender.handleLogs("Distribution server", "PROCESS",
      new LogStorekeeper(Common.DistributionServiceName, None, config.instance, collections.Log_Lines, config.logs))

    val workspace = GraphqlWorkspace(config, collections, dir, taskManager)
    val distribution = new Distribution(workspace, graphql)

    val selfUpdater = new SelfUpdater(collections, dir, workspace)
    selfUpdater.start()

    workspace.getProvidersInfo().foreach(_.foreach { providerInfo =>
      providerInfo.uploadState.filter(s => s).foreach(_ =>
        StateUploader(config.distribution, collections, dir,
          providerInfo.url, providerInfo.accessToken).start())
      providerInfo.autoUpdate.filter(u => u).foreach(_ =>
        AutoUpdater.start(providerInfo.distribution, workspace, workspace, workspace, taskManager))
    })

    var server = Http().newServerAt("0.0.0.0", config.network.port)
    config.network.ssl.foreach {
      log.info("Enable https")
      ssl => server = server.enableHttps(makeHttpsContext(ssl))
    }
    server.bind(distribution.route)
  } catch {
    case ex: Throwable =>
      log.error("Exception", ex)
      Utils.error(ex.toString)
  }

  def makeHttpsContext(config: SslConfig): HttpsConnectionContext = {
    val keyStore = KeyStore.getInstance("PKCS12")
    val keyStoreStream = new FileInputStream(new File(config.keyStoreFile))

    keyStore.load(keyStoreStream, config.keyStorePassword.map(_.toCharArray).getOrElse(null))

    val keyManagerFactory = KeyManagerFactory.getInstance("SunX509")
    keyManagerFactory.init(keyStore, config.keyStorePassword.map(_.toCharArray).getOrElse(null))

    val trustManagerFactory = TrustManagerFactory.getInstance("SunX509")
    trustManagerFactory.init(keyStore)

    val sslContext = SSLContext.getInstance("TLS")
    sslContext.init(keyManagerFactory.getKeyManagers, trustManagerFactory.getTrustManagers, new SecureRandom)
//    val parameters = sslContext.getDefaultSSLParameters
//    val protocols = parameters.getProtocols
//    log.info(s"Original SSL protocols: ${protocols.toSeq}")
//    val enabledProtocols = protocols.filter(_ != "TLSv1").filter(_ != "TLSv1.1")
//    if (enabledProtocols.length != protocols.length) {
//      log.info(s"Set SSL protocols: ${enabledProtocols.toSeq}")
//      parameters.setProtocols(enabledProtocols)
//    }
    ConnectionContext.httpsServer(sslContext)
  }
}