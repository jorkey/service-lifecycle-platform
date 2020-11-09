package distribution.uploaders

import java.net.URL
import java.util.Date
import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.http.scaladsl.server.directives.FutureDirectives
import akka.stream.Materializer
import org.slf4j.LoggerFactory
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import com.mongodb.client.model.Filters
import com.vyulabs.update.distribution.{DistributionDirectory, DistributionDirectoryClient, DistributionMain}
import com.vyulabs.update.utils.Utils.DateJson._
import distribution.mongo.DatabaseCollections
import spray.json.DefaultJsonProtocol

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.FiniteDuration

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 22.05.19.
  * Copyright FanDate, Inc.
  */

class StateUploader(collections: DatabaseCollections, distributionDirectory: DistributionDirectory, developerDirectoryUrl: URL, uploadIntervalSec: Int)
                   (implicit system: ActorSystem, materializer: Materializer, executionContext: ExecutionContext)  extends FutureDirectives with SprayJsonSupport { self =>
  private implicit val log = LoggerFactory.getLogger(this.getClass)

  private val developerClient = new DistributionDirectoryClient(developerDirectoryUrl)

  system.getScheduler.scheduleWithFixedDelay(
    FiniteDuration(uploadIntervalSec, TimeUnit.SECONDS), FiniteDuration(uploadIntervalSec, TimeUnit.SECONDS))(() => uploadState())

  private def uploadState(): Unit = {
    for {
      uploadStatus <- collections.State_UploadStatus

    } yield {

    }

  }

  /*private def getLastUploadState(component: String): Future[Date] = {
    for {
      uploadStatus <- collections.State_UploadStatus
      date <- uploadStatus.find(Filters.eq("component", component)).map(_.headOption.map(_.uploadStatus.lastUploadDate).getOrElse(new Date(0)))
    } yield date
  }

  private def uploadInstanceStates(fromDate: Future[Date]): Unit = {
    for {
      fromDate <- fromDate
      serviceStates <- collections.State_ServiceStates
      l <- serviceStates.find(Filters.gte())
    } yield {

    }
  }*/
}