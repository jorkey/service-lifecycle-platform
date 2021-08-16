package com.vyulabs.update.distribution.graphql.utils

import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import com.vyulabs.update.common.common.Common
import com.vyulabs.update.common.common.Common.{DistributionId, TaskId}
import com.vyulabs.update.common.config.DistributionConfig
import com.vyulabs.update.common.distribution.client.DistributionClient
import com.vyulabs.update.common.distribution.client.graphql.{AdministratorSubscriptionsCoder, BuilderQueriesCoder, GraphqlArgument, GraphqlMutation}
import com.vyulabs.update.common.distribution.server.{DistributionDirectory, InstallSettingsDirectory}
import com.vyulabs.update.common.info.{AccessToken, AccountRole, LogLine}
import com.vyulabs.update.common.process.ChildProcess
import com.vyulabs.update.common.utils.{IoUtils, ZipUtils}
import com.vyulabs.update.distribution.client.AkkaHttpClient
import com.vyulabs.update.distribution.common.AkkaTimer
import com.vyulabs.update.distribution.mongo.DatabaseCollections
import com.vyulabs.update.distribution.task.TaskManager
import org.slf4j.Logger
import spray.json.DefaultJsonProtocol._

import java.io.{File, IOException}
import java.nio.file.Files
import java.text.ParseException
import java.util.Date
import scala.concurrent.{ExecutionContext, Future, Promise}

trait RunBuilderUtils extends SprayJsonSupport {
  protected val directory: DistributionDirectory
  protected val collections: DatabaseCollections
  protected val config: DistributionConfig
  protected val taskManager: TaskManager

  protected val accountsUtils: AccountsUtils
  protected val stateUtils: StateUtils
  protected val distributionProvidersUtils: DistributionProvidersUtils

  protected implicit val executionContext: ExecutionContext
  protected implicit val system: ActorSystem

  implicit val timer = new AkkaTimer(system.scheduler)

  def runBuilder(task: TaskId, arguments: Seq[String])
                (implicit log: Logger): (Future[Unit], Option[() => Unit]) = {
    val accessToken = accountsUtils.encodeAccessToken(AccessToken(Common.BuilderServiceName))
    if (config.distribution == config.builder.distribution) {
      runLocalBuilder(task, config.builder.distribution, accessToken, arguments)
    } else {
      runRemoteBuilder(task, config.builder.distribution, accessToken, arguments)
    }
  }

  def runBuilderByRemoteDistribution(distribution: DistributionId, accessToken: String,
                                     arguments: Seq[String])(implicit log: Logger): TaskId = {
    val task = taskManager.create("Run builder by remote distribution",
      (task, logger) => {
        implicit val log = logger
        runLocalBuilder(task, distribution, accessToken, arguments)
      })
    task.task
  }

  private def runLocalBuilder(task: TaskId, distribution: DistributionId,
                              accessToken: String, arguments: Seq[String])
                             (implicit log: Logger): (Future[Unit], Option[() => Unit]) = {
    val process = for {
      distributionUrl <- {
        if (config.distribution != distribution) {
          for {
            accountInfo <- accountsUtils.getConsumerAccountInfo(distribution)
          } yield {
            accountInfo.getOrElse(throw new IOException(s"No consumer account ${distribution}")).properties.url
          }
        } else {
          Future(s"http://localhost:${config.network.port}")
        }
      }
      _ <- prepareBuilder(distribution, distributionUrl, accessToken)
      process <- {
        log.info(s"--------------------------- Start builder")
        ChildProcess.start("/bin/sh", s"./${Common.BuilderSh}" +: arguments,
          Map.empty[String, String] +
            ("distribution" -> distribution) +
            ("distributionUrl" -> distributionUrl) +
            ("accessToken" -> accessToken), directory.getBuilderDir(distribution))
      }
    } yield {
      val outputFinishedPromise = Promise[Unit]
      @volatile var logOutputFuture = Option.empty[Future[Unit]]
      process.readOutput(
        lines => {
          val logLines = lines.map(line => {
            try {
              val array = line._1.split(" ", 5)
              val dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
              val date = dateFormat.parse(s"${array(0)} ${array(1)}")
              LogLine(date, array(2), array(3), array(4), None)
            } catch {
              case e: ParseException =>
                LogLine(new Date, "INFO", "", line._1, None)
            }
          })
          logOutputFuture = Some(logOutputFuture.getOrElse(Future()).flatMap { _ =>
            stateUtils.addServiceLogs(config.distribution, Common.BuilderServiceName,
              Some(task), config.instance, process.getHandle().pid().toString, directory.getBuilderDir().toString, logLines).map(_ => ())
          })
        },
        exitCode => {
          logOutputFuture.getOrElse(Future()).flatMap { _ =>
            stateUtils.addServiceLogs(config.distribution, Common.BuilderServiceName,
              Some(task), config.instance, process.getHandle().pid().toString, directory.getBuilderDir().toString,
              Seq(LogLine(new Date, "", "PROCESS", s"Builder process terminated with status ${exitCode}", None)))
          }.andThen { case _ => outputFinishedPromise.success(Unit) }
        })
      (process, outputFinishedPromise.future)
    }
    val future = for {
      result <- process
        .flatMap { case (process, outputFinished) => outputFinished.map(_ => process) }
        .map(_.onTermination().map {
          case 0 => ()
          case error => throw new IOException(s"Builder process terminated with status ${error}")
        }).flatten
    } yield result
    (future, Some(() => process.map(_._1.terminate())))
  }

  private def runRemoteBuilder(task: TaskId, distribution: DistributionId, accessToken: String, arguments: Seq[String])
                              (implicit log: Logger): (Future[Unit], Option[() => Unit]) = {
    @volatile var distributionClient = Option.empty[DistributionClient[AkkaHttpClient.AkkaSource]]
    @volatile var remoteTaskId = Option.empty[TaskId]
    val future = for {
      client <- distributionProvidersUtils.getDistributionProviderInfo(distribution).map(provider => {
        new DistributionClient(new AkkaHttpClient(provider.url)) })
      remoteTask <- client.graphqlRequest(GraphqlMutation[TaskId]("runBuilder",
        Seq(GraphqlArgument("accessToken" -> accessToken), GraphqlArgument("arguments" -> arguments, "[String!]"))))
      logSource <- client.graphqlRequestSSE(AdministratorSubscriptionsCoder.subscribeTaskLogs(remoteTask))
      end <- {
        distributionClient = Some(client)
        remoteTaskId = Some(remoteTask)
        val result = Promise[Unit]()
        @volatile var logOutputFuture = Option.empty[Future[Unit]]
        logSource.map(line => {
          logOutputFuture = Some(logOutputFuture.getOrElse(Future()).flatMap { _ =>
            stateUtils.addServiceLogs(config.distribution, Common.DistributionServiceName,
              Some(task), config.instance, 0.toString, "", Seq(line.line)).map(_ => ())
          })
          for (terminationStatus <- line.line.terminationStatus) {
            if (terminationStatus) {
              logOutputFuture.foreach(_.andThen { case _ => result.success() })
            } else {
              logOutputFuture.foreach(_.andThen { case _ => result.failure(new IOException(s"Remote builder is failed")) })
            }
          }
        }).run()
        result.future
      }
    } yield end
    (future, Some(() => {
      remoteTaskId.map(remoteTaskId => distributionClient.foreach(
        _.graphqlRequest(GraphqlMutation[Boolean]("cancelTask", Seq(GraphqlArgument("task" -> remoteTaskId)))).map(_ => ())))
    }))
  }

  private def prepareBuilder(distribution: DistributionId, distributionUrl: String, accessToken: String)
                             (implicit log: Logger): Future[Unit] = {
    val builderDir = directory.getBuilderDir(distribution)
    if (new File(directory.getBuilderDir(distribution), Common.BuilderSh).exists()) {
      Future()
    } else {
      log.info(s"--------------------------- Initialize builder directory ${builderDir}")
      val httpClient = new AkkaHttpClient(distributionUrl)
      httpClient.accessToken = Some(accessToken)
      val distributionClient = new DistributionClient(httpClient)
      for {
        desiredVersion <- {
          distributionClient.graphqlRequest(BuilderQueriesCoder.getClientDesiredVersions(Seq(Common.ScriptsServiceName)))
            .map(_.headOption.getOrElse(throw new IOException("Client desired version for scripts is not defined")).version)
        }
        imageFile <- {
          val tmpFile = Files.createTempFile("", "").toFile
          distributionClient.downloadClientVersionImage(Common.ScriptsServiceName, desiredVersion, tmpFile).map(_ => tmpFile)
        }
        status <- {
          Future[Unit] {
            val tmpDir = Files.createTempDirectory("builder").toFile
            if (ZipUtils.unzip(imageFile, tmpDir)) {
              if (!IoUtils.copyFile(new File(tmpDir, "builder"), builderDir) ||
                !IoUtils.copyFile(new File(tmpDir, Common.UpdateSh), new File(builderDir, Common.UpdateSh))) {
                throw new IOException("Can't find updater script files")
              } else {
                log.info(s"--------------------------- Set execution permission")
                if (!builderDir.listFiles().forall { file => !file.getName.endsWith(".sh") || IoUtils.setExecuteFilePermissions(file) }) {
                  throw new IOException("Can't set execution permissions")
                } else {
                  log.info(s"--------------------------- Create settings directory")
                  new InstallSettingsDirectory(builderDir)
                }
              }
            } else {
              throw new IOException("Can't unzip scripts version image file")
            }
          }
        }
      } yield status
    }
  }
}
