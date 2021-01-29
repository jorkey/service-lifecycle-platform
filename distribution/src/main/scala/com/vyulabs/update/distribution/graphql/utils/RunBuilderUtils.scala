package com.vyulabs.update.distribution.graphql.utils

import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import com.vyulabs.update.common.common.Common.{DistributionName, TaskId}
import com.vyulabs.update.common.distribution.server.DistributionDirectory
import com.vyulabs.update.common.process.ChildProcess
import com.vyulabs.update.distribution.common.AkkaTimer
import com.vyulabs.update.distribution.mongo.DatabaseCollections
import org.slf4j.Logger

import scala.concurrent.{ExecutionContext, Future}

trait RunBuilderUtils extends StateUtils with SprayJsonSupport {
  protected val distributionName: DistributionName
  protected val dir: DistributionDirectory
  protected val collections: DatabaseCollections
  protected val builderExecutePath: String

  protected implicit val executionContext: ExecutionContext
  protected implicit val system: ActorSystem

  implicit val timer = new AkkaTimer(system.scheduler)

  def runBuilder(taskId: TaskId, arguments: Seq[String])(implicit log: Logger): TaskId = {
    null
  }

  def runLocalBuilder(taskId: TaskId, arguments: Seq[String])(implicit log: Logger): TaskId = {
    val process = ChildProcess.start(builderExecutePath, arguments).get
    process.handleOutput((line, nl) =>
      println(line)
    )
/*
    addServiceLogs()
    result(process.waitForTermination())

    for {

    } yield result
*/
    null
  }

  def runRemoteBuilder(taskId: TaskId, arguments: Seq[String])(implicit log: Logger): TaskId = {
    null
  }
}
