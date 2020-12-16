package com.vyulabs.update.distribution

import akka.actor.ActorSystem
import akka.http.scaladsl.{ConnectionContext, Http, HttpsConnectionContext}
import akka.stream.ActorMaterializer
import com.vyulabs.update.common.common.Common
import com.vyulabs.update.common.distribution.server.DistributionDirectory
import com.vyulabs.update.common.lock.SmartFilesLocker
import com.vyulabs.update.common.logger.TraceAppender
import com.vyulabs.update.common.utils.Utils
import com.vyulabs.update.distribution.config.{DistributionConfig, SslConfig}
import com.vyulabs.update.distribution.graphql.{Graphql, GraphqlWorkspace}
import com.vyulabs.update.distribution.loaders.StateUploader
import com.vyulabs.update.distribution.logger.LogStorer
import com.vyulabs.update.distribution.mongo.{DatabaseCollections, MongoDb}
import org.slf4j.LoggerFactory

import java.io.{File, FileInputStream}
import java.security.{KeyStore, SecureRandom}
import javax.net.ssl.{KeyManagerFactory, SSLContext, TrustManagerFactory}
import scala.concurrent.ExecutionContext

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

  def usage() = "Arguments: run"

  try {
    val command = args(0)

    implicit val executionContext = ExecutionContext.fromExecutor(null, ex => log.error("Uncatched exception", ex))

    implicit val filesLocker = new SmartFilesLocker()

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

        val selfUpdater = new SelfUpdater(collections, dir)
        selfUpdater.start()

        val workspace = GraphqlWorkspace(config.distributionName, config.versionHistory, config.faultReportsConfig, collections, dir)
        val distribution = new Distribution(workspace, graphql)

        var server = Http().newServerAt("0.0.0.0", config.network.port)
        config.network.ssl.foreach {
          log.info("Enable https")
          ssl => server = server.enableHttps(makeHttpsContext(ssl))
        }
        server.bind(distribution.route)
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