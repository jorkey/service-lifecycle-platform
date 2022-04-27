package com.vyulabs.update.distribution.graphql.utils

import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import com.vyulabs.update.common.common.Common.{DistributionId, ServiceId, TaskId}
import com.vyulabs.update.common.common.{Common, Misc}
import com.vyulabs.update.common.config.{DistributionConfig, NamedStringValue}
import com.vyulabs.update.common.distribution.client.DistributionClient
import com.vyulabs.update.common.distribution.client.graphql.{AdministratorSubscriptionsCoder, BuilderQueriesCoder, GraphqlArgument, GraphqlMutation}
import com.vyulabs.update.common.distribution.server.DistributionDirectory
import com.vyulabs.update.common.info.{AccessToken, LogLine}
import com.vyulabs.update.common.logs.LogFormat
import com.vyulabs.update.common.process.ChildProcess
import com.vyulabs.update.common.utils.{IoUtils, ZipUtils}
import com.vyulabs.update.distribution.client.AkkaHttpClient
import com.vyulabs.update.distribution.common.AkkaTimer
import com.vyulabs.update.distribution.mongo.DatabaseCollections
import org.slf4j.Logger
import spray.json.DefaultJsonProtocol._
import spray.json._

import java.io.{File, IOException}
import java.nio.file.Files
import java.text.ParseException
import java.util.Date
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success}

trait RunBuilderUtils extends SprayJsonSupport {
  protected val directory: DistributionDirectory
  protected val collections: DatabaseCollections
  protected val config: DistributionConfig

  protected val accountsUtils: AccountsUtils
  protected val logUtils: LogUtils
  protected val tasksUtils: TasksUtils
  protected val distributionProvidersUtils: DistributionProvidersUtils
  protected val configBuilderUtils: ConfigBuilderUtils

  protected implicit val executionContext: ExecutionContext
  protected implicit val system: ActorSystem

  implicit val timer = new AkkaTimer(system.scheduler)

  def runDeveloperBuilder(task: TaskId, service: ServiceId, arguments: Seq[String])
                         (implicit log: Logger): (Future[Unit], Option[() => Unit]) = {
    val future = for {
      config <- configBuilderUtils.getBuildDeveloperServiceMergedConfig(service)
    } yield {
      val args = arguments ++ Seq(
        s"sourceRepositories=${config.repositories.toJson.compactPrint}",
        s"privateFiles=${config.privateFiles.toJson.compactPrint}",
        s"macroValues=${config.macroValues.toJson.compactPrint}")
      runBuilder(task, config.distribution, config.environment, args, service)
    }
    val result = future.map(_._1).flatten
    val cancel = Some(() =>
      if (future.isCompleted && future.value.get.isSuccess) future.value.get.get._2.foreach(_.apply()))
    (result, cancel)
  }

  def runClientBuilder(task: TaskId, service: ServiceId, arguments: Seq[String])
                      (implicit log: Logger): (Future[Unit], Option[() => Unit]) = {
    val future = for {
      config <- configBuilderUtils.getBuildClientServiceMergedConfig(service)
    } yield {
      val args = arguments ++ Seq(
        s"settingsRepositories=${config.repositories.toJson.compactPrint}",
        s"privateFiles=${config.privateFiles.toJson.compactPrint}",
        s"macroValues=${config.macroValues.toJson.compactPrint}")
      runBuilder(task, config.distribution, config.environment, args, service)
    }
    val result = future.map(_._1).flatten
    val cancel = Some(() =>
      if (future.isCompleted && future.value.get.isSuccess) future.value.get.get._2.foreach(_.apply()))
    (result, cancel)
  }

  def runBuilder(task: TaskId, distribution: DistributionId,
                 environment: Seq[NamedStringValue], arguments: Seq[String], service: ServiceId)
                (implicit log: Logger): (Future[Unit], Option[() => Unit]) = {
    val accessToken = accountsUtils.encodeAccessToken(AccessToken(Common.BuilderServiceName))
    if (config.distribution == distribution) {
      runLocalBuilder(task, distribution, accessToken, environment, arguments)
    } else {
      runRemoteBuilder(task, distribution, accessToken, environment, arguments, service)
    }
  }

  def runBuilderByRemoteDistribution(distribution: DistributionId, accessToken: String,
                                     arguments: Seq[String], environment: Seq[NamedStringValue],
                                     service: ServiceId)
                                    (implicit log: Logger): Future[TaskId] = {
    tasksUtils.createTask(
      "RunBuilderByRemoteDistribution",
      Seq(TaskParameter("distribution", distribution),
        TaskParameter("accessToken", accessToken),
        TaskParameter("environment", Misc.seqToCommaSeparatedString(environment)),
        TaskParameter("arguments", Misc.seqToCommaSeparatedString(arguments))),
      Seq(service),
      (task, logger) => {
        implicit val log = logger
        runLocalBuilder(task, distribution, accessToken, environment, arguments)
      }).map(_.taskId)
  }

  private def runLocalBuilder(task: TaskId, distribution: DistributionId,
                              accessToken: String, environment: Seq[NamedStringValue],
                              arguments: Seq[String])
                             (implicit log: Logger): (Future[Unit], Option[() => Unit]) = {
    log.info(s"Run builder on local distribution server")
    val process = for {
      distributionUrl <- {
        if (config.distribution != distribution) {
          for {
            accountInfo <- accountsUtils.getConsumerAccountInfo(distribution)
          } yield {
            accountInfo.getOrElse(throw new IOException(s"No consumer account '${distribution}''")).properties.url
          }
        } else {
          Future(s"${if (config.network.ssl.isDefined) "https" else "http"}://${config.network.host}:${config.network.port}")
        }
      }
      _ <- prepareBuilder(distribution, distributionUrl, accessToken)
      process <- {
        log.info(s"--------------------------- Start builder")
        ChildProcess.start("/bin/bash", s"./${Common.BuilderSh}" +: arguments,
          environment.foldLeft(Map.empty[String, String])((m, e) => m + (e.name -> e.value)) +
            ("distribution" -> distribution) +
            ("distributionUrl" -> distributionUrl) +
            ("accessToken" -> accessToken),
          directory.getBuilderDir(distribution))
      }
    } yield {
      val outputFinishedPromise = Promise[Unit]
      process.readOutput(
        lines => {
          val logLines = lines.map(line => {
            try {
              LogFormat.parse(line._1)
            } catch {
              case e: ParseException =>
                LogLine(new Date, "INFO", "", line._1, None)
            }
          })
          logUtils.addLogs(Common.BuilderServiceName,
            config.instance, directory.getBuilderDir().toString, process.getHandle().pid().toString, Some(task), logLines).map(_ => ())
        },
        exitCode => {
          logUtils.addLogs(Common.BuilderServiceName,
            config.instance, directory.getBuilderDir().toString, process.getHandle().pid().toString, Some(task),
            Seq(LogLine(new Date, "", "PROCESS", s"Builder process terminated with status ${exitCode}", None)))
            .andThen { case _ => outputFinishedPromise.success(Unit) }
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

  private def runRemoteBuilder(task: TaskId, distribution: DistributionId, accessToken: String,
                               environment: Seq[NamedStringValue], arguments: Seq[String], service: ServiceId)
                              (implicit log: Logger): (Future[Unit], Option[() => Unit]) = {
    log.info(s"Run builder on remote distribution server '${distribution}''")
    @volatile var distributionClient = Option.empty[DistributionClient[AkkaHttpClient.AkkaSource]]
    @volatile var remoteTaskId = Option.empty[TaskId]
    val future = for {
      client <- distributionProvidersUtils.getDistributionProviderInfo(distribution).map(provider => {
        new DistributionClient(new AkkaHttpClient(provider.url, Some(provider.accessToken))) })
      remoteTask <- client.graphqlRequest(GraphqlMutation[TaskId]("runBuilder",
        Seq(GraphqlArgument("accessToken" -> accessToken),
            GraphqlArgument("environment" -> environment, "[NamedStringValueInput!]"),
            GraphqlArgument("arguments" -> arguments, "[String!]"),
            GraphqlArgument("service" -> service),
        )))
      logSource <- client.graphqlRequestWS(AdministratorSubscriptionsCoder.subscribeTaskLogs(remoteTask))
      end <- {
        distributionClient = Some(client)
        remoteTaskId = Some(remoteTask)
        val result = Promise[Unit]()
        @volatile var logOutputFuture = Future()
        logSource.map(lines => {
          logUtils.addLogs(Common.DistributionServiceName,
            config.instance, "", 0.toString, Some(task),
            lines.map(line => LogLine(
              time = line.time,
              level = line.level,
              unit = line.unit,
              message = line.message,
              terminationStatus = None))).map(_ => ())
          for (terminationStatus <- lines.lastOption.map(_.terminationStatus).flatten) {
            if (terminationStatus) {
              result.trySuccess()
            } else {
              result.tryFailure(new IOException(s"Remote builder is failed"))
            }
          }
        }).run()
          .onComplete {
            case Success(_) =>
              if (!result.isCompleted) {
                for (remoteTaskId <- remoteTaskId) {
                  log.info(s"Remote task ${remoteTaskId} log is completed")

                  def complete(): Unit = {
                    result.tryFailure(new IOException(s"Unexpected completion of remote task ${remoteTaskId} log"))
                  }

                  system.scheduler.scheduleOnce(FiniteDuration(5, TimeUnit.SECONDS))(complete())
                }
              }
            case Failure(ex) =>
              log.error("Subscription flow error", ex)
              result.tryFailure(ex)
          }
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
      log.info(s"Initialize builder directory ${builderDir}")
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
                log.info(s"Set execution permission")
                if (!builderDir.listFiles().forall { file => !file.getName.endsWith(".sh") || IoUtils.setExecuteFilePermissions(file) }) {
                  throw new IOException("Can't set execution permissions")
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
