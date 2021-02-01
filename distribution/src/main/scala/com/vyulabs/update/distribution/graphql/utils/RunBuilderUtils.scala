package com.vyulabs.update.distribution.graphql.utils

import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import com.vyulabs.update.common.common.Common
import com.vyulabs.update.common.common.Common.{DistributionName, InstanceId, TaskId}
import com.vyulabs.update.common.distribution.server.DistributionDirectory
import com.vyulabs.update.common.info.LogLine
import com.vyulabs.update.common.process.ChildProcess
import com.vyulabs.update.distribution.common.AkkaTimer
import com.vyulabs.update.distribution.mongo.DatabaseCollections
import org.slf4j.Logger

import scala.concurrent.{ExecutionContext, Future}

trait RunBuilderUtils extends StateUtils with SprayJsonSupport {
  protected val instanceId: InstanceId
  protected val distributionName: DistributionName
  protected val dir: DistributionDirectory
  protected val collections: DatabaseCollections
  protected val builderExecutePath: String

  protected implicit val executionContext: ExecutionContext
  protected implicit val system: ActorSystem

  implicit val timer = new AkkaTimer(system.scheduler)

  def runBuilder(taskId: TaskId, arguments: Seq[String])(implicit log: Logger): (Future[Boolean], () => Unit) = {
    null
  }

  def runLocalBuilder(taskId: TaskId, arguments: Seq[String])(implicit log: Logger): (Future[Boolean], () => Unit) = {
    val process = ChildProcess.start(builderExecutePath, arguments).get
    process.handleOutput(lines => {
      val logLines = lines.map(line => {
        val array = line._1.split(" ", 6)
        val dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
        val date = dateFormat.parse(s"${array(0)} ${array(1)}")
        LogLine(date, array(2), Some(array(3)), array(5), None))
      })
      addServiceLogs(distributionName, Common.DistributionServiceName, Some(taskId), instanceId, process.getHandle().pid().toString, builderExecutePath, logLines)
    })
/*
    result(process.waitForTermination())

    for {

    } yield result
*/
    null
  }

  def runLocalBuilderByRemoveDistribution(taskId: TaskId, arguments: Seq[String])(implicit log: Logger): Boolean = {
    false
  }

  def runRemoteBuilder(taskId: TaskId, arguments: Seq[String])(implicit log: Logger): (Future[Boolean], () => Unit) = {
    null
  }
}
