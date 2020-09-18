package distribution.utils

import java.io.{File, IOException}
import java.nio.file.StandardOpenOption
import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes.NotFound
import akka.http.scaladsl.model.{HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives.{complete, failWith, fileUpload, getFromFile, _}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.FileInfo
import akka.pattern.after
import akka.stream.Materializer
import akka.stream.scaladsl.{FileIO, Sink, Source}
import akka.util.ByteString
import com.vyulabs.update.lock.{SmartFileLock, SmartFilesLocker}
import org.slf4j.LoggerFactory
import spray.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success}

trait PutUtils extends SprayJsonSupport {
  private implicit val log = LoggerFactory.getLogger(this.getClass)

  implicit val system: ActorSystem
  implicit val materializer: Materializer
  implicit val filesLocker: SmartFilesLocker

  def fileUploadWithLock(fieldName: String, targetFile: File): Route = {
    fileUpload(fieldName) {
      case (fileInfo, byteSource) =>
        fileWriteWithLock(byteSource, targetFile)
    }
  }

  private def fileWriteWithLock(byteSource: Source[ByteString, Any], targetFile: File, completePromise: Option[Promise[Unit]] = None)
                       (implicit system: ActorSystem, materializer: Materializer, filesLocker: SmartFilesLocker): Route = {
    filesLocker.tryLock(targetFile, false) match {
      case Some(lock) =>
        val sink = FileIO.toPath(targetFile.toPath, Set(StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING))
        val result = byteSource.runWith(sink)
        onSuccess(result) { result =>
          lock.release()
          completePromise.foreach(_.success())
          result.status match {
            case Success(_) =>
              complete(StatusCodes.OK)
            case Failure(ex) =>
              failWith(new IOException(s"Write ${targetFile} error", ex))
          }
        }
      case None =>
        //log.info(s"Can't lock ${targetFile} in exclusively mode. Retry attempt after pause")
        val result = Source.tick(FiniteDuration(100, TimeUnit.MILLISECONDS), FiniteDuration(100, TimeUnit.MILLISECONDS), Unit)
          .take(1).runWith(Sink.ignore)
        onComplete(result) { _ =>
          fileWriteWithLock(byteSource, targetFile)
        }
    }
  }

  def uploadFileToJson(fieldName: String, processUpload: (JsValue) => Route): Route = {
    fileUpload(fieldName) {
      case (_, byteSource) =>
        val sink = Sink.fold[ByteString, ByteString](ByteString())(_ ++ _)
        val result = byteSource.runWith(sink)
        onSuccess(result) { result =>
          processUpload(result.decodeString("utf8").parseJson)
        }
    }
  }

  def uploadFileToSource(fieldName: String, source: (FileInfo, Source[ByteString, Any]) => Route): Route = {
    fileUpload(fieldName) {
      case (fileInfo, byteSource) =>
        source(fileInfo, byteSource)
    }
  }

  def overwriteFileContentWithLock(targetFile: File, replaceContent: (Option[ByteString]) => ByteString): Future[Unit] = {
    val promise = Promise[Unit]()
    try {
      filesLocker.tryLock(targetFile, false) match {
        case Some(lock) =>
          def write(oldContent: Option[ByteString]): Unit = {
            val newContent = replaceContent(oldContent)
            val sink = FileIO.toPath(targetFile.toPath, Set(StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING))
            val outputFuture = Source.single(newContent).runWith(sink)
            outputFuture.onComplete {
              case Success(result) =>
                lock.release()
                result.status match {
                  case Success(_) =>
                    promise.success()
                  case Failure(ex) =>
                    promise.failure(ex)
                }
              case Failure(ex) =>
                lock.release()
                promise.failure(ex)
            }
          }
          if (targetFile.exists()) {
            val inputFuture = FileIO.fromPath(targetFile.toPath).runWith(Sink.fold[ByteString, ByteString](ByteString())(_ ++ _))
            inputFuture.onComplete {
              case Success(bytes) =>
                write(Some(bytes))
              case Failure(ex) =>
                lock.release()
                promise.failure(ex)
            }
          } else {
            write(None)
          }
        case None =>
          log.info(s"Can't lock ${targetFile} in not shared mode. Retry attempt after pause")
          after(FiniteDuration(100, TimeUnit.MILLISECONDS), system.scheduler)(overwriteFileContentWithLock(targetFile, replaceContent))
      }
    } catch {
      case ex: Exception =>
        promise.failure(ex)
    }
    promise.future
  }
}
