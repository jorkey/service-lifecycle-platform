package distribution.loaders

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.http.scaladsl.server.directives.FutureDirectives
import akka.stream.Materializer
import org.slf4j.LoggerFactory
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.mongodb.client.model.{Filters, Sorts, Updates}
import com.vyulabs.update.distribution.DistributionDirectory
import distribution.mongo.{DatabaseCollections}
import spray.json._

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success}

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 11.11.20.
  * Copyright FanDate, Inc.
  */

class StateUploader(collections: DatabaseCollections, distributionDirectory: DistributionDirectory, uploadIntervalSec: Int,
                    graphqlMutationRequest: (String, Map[String, JsValue]) => Future[Unit], uploadRequest: (String, Source[ByteString, Unit]) => Future[Unit])
                   (implicit system: ActorSystem, materializer: Materializer, executionContext: ExecutionContext)  extends FutureDirectives with SprayJsonSupport { self =>
  private implicit val log = LoggerFactory.getLogger(this.getClass)

  system.getScheduler.scheduleWithFixedDelay(
    FiniteDuration(1, TimeUnit.SECONDS), FiniteDuration(uploadIntervalSec, TimeUnit.SECONDS))(() => uploadState())

  private def uploadState(): Unit = {
    for {
      uploadStates <- uploadServicesStates()

    } yield {

    }

  }

  private def uploadServicesStates(): Future[Unit] = {
    for {
      serviceStates <- collections.State_ServiceStates
      fromSequence <- getLastUploadSequence(serviceStates.getName())
      newStatesDocuments <- serviceStates.find(Filters.gt("sequence", fromSequence), sort = Some(Sorts.ascending("sequence")))
      newStates <- Future(newStatesDocuments.map(_.state))
      if !newStates.isEmpty
      result <- graphqlMutationRequest("setServicesState", Map("state" -> newStates.toJson)).
        andThen {
          case Success(_) =>
            setLastUploadSequence(serviceStates.getName(), newStatesDocuments.last.sequence)
          case Failure(ex) =>
            setLastUploadError(serviceStates.getName(), ex.getMessage)
        }
    } yield result
  }

  private def uploadFaultReports(): Future[Unit] = {
    for {
      faultReports <- collections.State_FaultReports
      fromSequence <- getLastUploadSequence(faultReports.getName())
      newReportsDocuments <- faultReports.find(Filters.gt("_id", fromSequence), sort = Some(Sorts.ascending("_id")))
      newReports <- Future(newReportsDocuments.map(_.fault))
      if !newReports.isEmpty
      result <- uploadRequest("upload_fault_report", null). // TODO graphql
        andThen {
          case Success(_) =>
            setLastUploadSequence(faultReports.getName(), newReportsDocuments.last._id)
          case Failure(ex) =>
            setLastUploadError(faultReports.getName(), ex.getMessage)
        }
    } yield result
  }

  private def getLastUploadSequence(component: String): Future[Long] = {
    for {
      uploadStatus <- collections.State_UploadStatus
      sequence <- uploadStatus.find(Filters.eq("component", component)).map(_.headOption.map(_.uploadStatus.lastUploadSequence).getOrElse(0L))
    } yield sequence
  }

  private def setLastUploadSequence(component: String, sequence: Long): Future[Boolean] = {
    for {
      uploadStatus <- collections.State_UploadStatus
      result <- uploadStatus.updateOne(Filters.eq("component", component),
        Updates.combine(Updates.set("uploadStatus.lastUploadSequence", sequence), Updates.set("uploadStatus.lastError", JsNull)))
        .map(r => r.getModifiedCount > 0)
    } yield result
  }

  private def setLastUploadError(component: String, error: String): Future[Unit] = {
    for {
      uploadStatus <- collections.State_UploadStatus
      date <- uploadStatus.updateOne(Filters.eq("component", component),
        Updates.set("uploadStatus.lastError", error))
        .map(r => r.getModifiedCount > 0)
    } yield date
  }
}