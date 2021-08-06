package com.vyulabs.update.distribution.graphql.utils

import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import com.vyulabs.update.common.common.Common
import com.vyulabs.update.common.common.Common.{DistributionId, TaskId}
import com.vyulabs.update.common.config.DistributionConfig
import com.vyulabs.update.common.distribution.client.DistributionClient
import com.vyulabs.update.common.distribution.client.graphql.{AdministratorSubscriptionsCoder, GraphqlArgument, GraphqlMutation}
import com.vyulabs.update.common.distribution.server.DistributionDirectory
import com.vyulabs.update.common.info.{AccessToken, AccountRole, LogLine}
import com.vyulabs.update.common.process.ChildProcess
import com.vyulabs.update.distribution.client.AkkaHttpClient
import com.vyulabs.update.distribution.common.AkkaTimer
import com.vyulabs.update.distribution.mongo.DatabaseCollections
import com.vyulabs.update.distribution.task.TaskManager
import org.slf4j.Logger
import spray.json.DefaultJsonProtocol._

import java.io.IOException
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

  protected implicit val executionContext: ExecutionContext
  protected implicit val system: ActorSystem

  protected val distributionProvidersUtils: DistributionProvidersUtils

  implicit val timer = new AkkaTimer(system.scheduler)

  def runBuilder(task: TaskId, arguments: Seq[String])(implicit log: Logger): (Future[Unit], Option[() => Unit]) = {
    val args = arguments :+
      config.network.url :+
      s"${accountsUtils.encodeAccessToken(AccessToken(Common.BuilderServiceName, Seq(AccountRole.Builder), None))}"
    if (config.distribution == config.builder.distribution) {
      runLocalBuilder(task, args)
    } else {
      runRemoteBuilder(task, args, config.builder.distribution)
    }
  }

  def runBuilderByRemoteDistribution(arguments: Seq[String])(implicit log: Logger): TaskId = {
    val task = taskManager.create("Run builder by remote distribution",
      (task, logger) => {
        implicit val log = logger
        runLocalBuilder(task, arguments)
      })
    task.task
  }

  private def runLocalBuilder(task: TaskId, arguments: Seq[String])
                             (implicit log: Logger): (Future[Unit], Option[() => Unit]) = {
    val outputFinishedPromise = Promise[Unit]
    @volatile var logOutputFuture = Option.empty[Future[Unit]]
    val process = for {
      process <- ChildProcess.start("/bin/sh", s"./${Common.BuilderSh}" +: arguments,
        Map.empty, directory.getBuilderDir())
    } yield {
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
      process
    }
    val future = outputFinishedPromise.future.flatMap(_ => process.map(_.onTermination().map {
      case 0 => ()
      case error => throw new IOException(s"Builder process terminated with status ${error}")
    }).flatten)
    (future, Some(() => process.map(_.terminate())))
  }

  private def runRemoteBuilder(task: TaskId, arguments: Seq[String], distribution: DistributionId)
                              (implicit log: Logger): (Future[Unit], Option[() => Unit]) = {
    @volatile var distributionClient = Option.empty[DistributionClient[AkkaHttpClient.AkkaSource]]
    @volatile var remoteTaskId = Option.empty[TaskId]
    val future = for {
      client <- distributionProvidersUtils.getDistributionProviderInfo(distribution).map(provider => {
        new DistributionClient(new AkkaHttpClient(provider.url)) })
      remoteTask <- client.graphqlRequest(GraphqlMutation[TaskId]("runBuilder",
        Seq(GraphqlArgument("arguments" -> arguments, "[String!]"))))
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
}
