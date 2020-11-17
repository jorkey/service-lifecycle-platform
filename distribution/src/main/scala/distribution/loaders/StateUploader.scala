package distribution.loaders

import java.io.{File, IOException}
import java.net.URL
import java.util.concurrent.TimeUnit

import akka.actor.{ActorSystem, Cancellable}
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding.Post
import akka.http.scaladsl.server.directives.FutureDirectives
import akka.stream.{IOResult, Materializer}
import org.slf4j.LoggerFactory
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, Multipart}
import akka.stream.scaladsl.{FileIO, Source}
import akka.util.ByteString
import com.mongodb.client.model.{Filters, Sorts, Updates}
import com.vyulabs.update.common.Common
import com.vyulabs.update.common.Common.InstanceId
import com.vyulabs.update.distribution.DistributionDirectory
import com.vyulabs.update.distribution.DistributionWebPaths.graphqlPathPrefix
import com.vyulabs.update.info.{ClientServiceState, DirectoryServiceState}
import distribution.mongo.{DatabaseCollections, ServiceStateDocument}
import spray.json._
import spray.json.DefaultJsonProtocol._

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success}

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 11.11.20.
  * Copyright FanDate, Inc.
  */

trait UploadRequests {
  def graphqlMutationRequest(command: String, arguments: Map[String, JsValue]): Future[Unit]
  def fileUploadRequest(path: String, fileName: String, length: Long, source: Source[ByteString, Future[IOResult]]): Future[Unit]
}

class StateUploader(collections: DatabaseCollections, distributionDirectory: DistributionDirectory, uploadIntervalSec: Int, uploadRequests: UploadRequests)
                   (implicit system: ActorSystem, materializer: Materializer, executionContext: ExecutionContext)  extends FutureDirectives with SprayJsonSupport { self =>
  private implicit val log = LoggerFactory.getLogger(this.getClass)

  var task = Option.empty[Cancellable]

  def setSelfStates(instanceId: InstanceId, homeDirectory: File, builderDirectory: Option[String], installerDirectory: Option[String]): Unit = {
    def addState(state: ClientServiceState): Future[com.mongodb.reactivestreams.client.Success] = {
      for {
        collection <- collections.State_ServiceStates
        id <- collections.getNextSequence(collection.getName())
        result <- collection.insert(ServiceStateDocument(id, state))
      } yield result
    }
    Future.sequence(Seq(
      addState(ClientServiceState(Common.OwnClient, instanceId,
        DirectoryServiceState.getServiceInstanceState(Common.DistributionServiceName, homeDirectory))),
      addState(ClientServiceState(Common.OwnClient, instanceId,
        DirectoryServiceState.getServiceInstanceState(Common.ScriptsServiceName, homeDirectory))),
    ) ++ builderDirectory.map(builderDirectory => Seq(
      addState(ClientServiceState(Common.OwnClient, instanceId,
        DirectoryServiceState.getServiceInstanceState(Common.BuilderServiceName, new File(homeDirectory, builderDirectory)))),
      addState(ClientServiceState(Common.OwnClient, instanceId,
        DirectoryServiceState.getServiceInstanceState(Common.ScriptsServiceName, new File(homeDirectory, builderDirectory)))))).getOrElse(Seq.empty)
      ++ installerDirectory.map(installerDirectory => Seq(
      addState(ClientServiceState(Common.OwnClient, instanceId,
        DirectoryServiceState.getServiceInstanceState(Common.InstallerServiceName, new File(homeDirectory, installerDirectory)))),
      addState(ClientServiceState(Common.OwnClient, instanceId,
        DirectoryServiceState.getServiceInstanceState(Common.ScriptsServiceName, new File(homeDirectory, installerDirectory)))))).getOrElse(Seq.empty)
    )
  }

  def start(): Unit = {
    task = Some(system.scheduler.scheduleOnce(FiniteDuration(1, TimeUnit.SECONDS))(uploadState()))
  }

  def stop(): Unit = {
    for (task <- task) {
      task.cancel()
      this.task = None
    }
  }

  private def uploadState(): Unit = {
    val result = for {
      _ <- uploadServiceStates()
      _ <- uploadFaultReports()
    } yield {}
    result.andThen { case _ => system.getScheduler.scheduleOnce(FiniteDuration(uploadIntervalSec, TimeUnit.SECONDS))(uploadState()) }
  }

  private def uploadServiceStates(): Future[Unit] = {
    for {
      serviceStates <- collections.State_ServiceStates
      fromSequence <- getLastUploadSequence(serviceStates.getName())
      newStatesDocuments <- serviceStates.find(Filters.gt("sequence", fromSequence), sort = Some(Sorts.ascending("sequence")))
      newStates <- Future(newStatesDocuments.map(_.state))
    } yield {
      if (!newStates.isEmpty) {
        uploadRequests.graphqlMutationRequest("setServicesState", Map("state" -> newStates.toJson)).
          onComplete {
            case Success(_) =>
              setLastUploadSequence(serviceStates.getName(), newStatesDocuments.last.sequence)
            case Failure(ex) =>
              setLastUploadError(serviceStates.getName(), ex.getMessage)
          }
      } else {
        Promise[Unit].success(Unit).future
      }
    }
  }

  private def uploadFaultReports(): Future[Unit] = {
    for {
      faultReports <- collections.State_FaultReports
      fromSequence <- getLastUploadSequence(faultReports.getName())
      newReportsDocuments <- faultReports.find(Filters.gt("_id", fromSequence), sort = Some(Sorts.ascending("_id")))
      newReports <- Future(newReportsDocuments.map(_.fault))
    } yield {
      if (!newReports.isEmpty) {
        Future.sequence(newReports.map(report => {
          val file = distributionDirectory.getFaultReportFile(report.faultId)
          uploadRequests.fileUploadRequest("upload_fault_report", file.getName, file.length(), FileIO.fromPath(file.toPath)).
            andThen {
              case Success(_) =>
                setLastUploadSequence(faultReports.getName(), newReportsDocuments.last._id)
              case Failure(ex) =>
                setLastUploadError(faultReports.getName(), ex.getMessage)
            }
        })).map(_ => Unit)
      } else {
        Promise[Unit].success(Unit).future
      }
    }
  }

  private def getLastUploadSequence(component: String): Future[Long] = {
    for {
      uploadStatus <- collections.State_UploadStatus
      sequence <- uploadStatus.find(Filters.eq("component", component)).map(_.headOption.map(_.uploadStatus.lastUploadSequence).flatten.getOrElse(-1L))
    } yield sequence
  }

  private def setLastUploadSequence(component: String, sequence: Long): Future[Boolean] = {
    for {
      uploadStatus <- collections.State_UploadStatus
      result <- uploadStatus.updateOne(Filters.eq("component", component),
        Updates.combine(Updates.set("uploadStatus.lastUploadSequence", sequence), Updates.unset("uploadStatus.lastError")))
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

object StateUploader {
  def apply(collections: DatabaseCollections, distributionDirectory: DistributionDirectory, uploadIntervalSec: Int, developerDistributionUrl: URL)
           (implicit system: ActorSystem, materializer: Materializer, executionContext: ExecutionContext): StateUploader = {

    new StateUploader(collections, distributionDirectory, uploadIntervalSec, new UploadRequests() {
      def graphqlMutationRequest(command: String, arguments: Map[String, JsValue]): Future[Unit] = {
        for {
          response <- Http(system).singleRequest(
            Post(developerDistributionUrl.toString + "/" + graphqlPathPrefix,
              HttpEntity(ContentTypes.`application/json`, s"mutation { ${command} ( ${arguments.toJson} ) }".getBytes())))
          entity <- response.entity.dataBytes.runFold(ByteString())(_ ++ _)
        } yield {
          val response = entity.decodeString("utf8")
          if (response != s"""{"data":{"${command}":true}}""") {
            throw new IOException(s"Unexpected response from server: ${response}")
          }
        }
      }

      override def fileUploadRequest(path: String, fileName: String, length: Long, source: Source[ByteString, Future[IOResult]]): Future[Unit] = {
        val multipartForm =
          Multipart.FormData(Multipart.FormData.BodyPart(
            "instances-state",
            HttpEntity(ContentTypes.`application/octet-stream`, length, source),
            Map("filename" -> fileName)))
        for {
          response <- Http(system).singleRequest(Post(developerDistributionUrl.toString + "/" + path, multipartForm))
          entity <- response.entity.dataBytes.runFold(ByteString())(_ ++ _)
        } yield {
          val response = entity.decodeString("utf8")
          if (response != "Success") {
            throw new IOException(s"Unexpected response from server: ${response}")
          }
        }
      }
    })
  }
}