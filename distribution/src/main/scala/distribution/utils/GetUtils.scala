package distribution.utils

import java.io.{File}
import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes.NotFound
import akka.http.scaladsl.model.{HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives.{complete, failWith, fileUpload, getFromFile, _}
import akka.http.scaladsl.server.Route
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

trait GetUtils extends SprayJsonSupport {
  private implicit val log = LoggerFactory.getLogger(this.getClass)

  implicit val system: ActorSystem
  implicit val materializer: Materializer
  implicit val filesLocker: SmartFilesLocker

  def parseJsonFileWithLock[T](file: File)(implicit format: RootJsonFormat[T]): Future[Option[T]] = {
    val promise = Promise[Option[T]]()
    getFileContentWithLock(file).onComplete {
      case Success(bytes) =>
        bytes match {
          case Some(bytes) =>
            try {
              val desiredVersions = bytes.decodeString("utf8").parseJson.convertTo[T]
              promise.success(Some(desiredVersions))
            } catch {
              case ex: Exception =>
                promise.failure(ex)
            }
          case None =>
            promise.success(None)
        }
      case Failure(ex) =>
        promise.failure(ex)
    }
    promise.future
  }

  def getFileContentWithLock(targetFile: File): Future[Option[ByteString]] = {
    val promise = Promise[Option[ByteString]]()
    log.debug(s"31 ${targetFile}")
    if (targetFile.exists()) {
      log.debug("32")
      try {
        filesLocker.tryLock(targetFile, true) match {
          case Some(lock) =>
            log.debug("32-")
            val future = FileIO.fromPath(targetFile.toPath).runWith(Sink.fold[ByteString, ByteString](ByteString())((b1, b2) => {
              b1 ++ b2
            }))
            future.onComplete {
              case Success(futureBytes) =>
                lock.release()
                promise.success(Some(futureBytes))
              case Failure(ex) =>
                lock.release()
                promise.failure(ex)
            }
          case None =>
            log.info(s"Can't lock ${targetFile} in shared mode. Retry attempt after pause")
            after(FiniteDuration(100, TimeUnit.MILLISECONDS), system.scheduler)(getFileContentWithLock(targetFile))
        }
      } catch {
        case ex: Exception =>
          promise.failure(ex)
      }
    } else {
      log.debug("33")
      promise.success(None)
    }
    promise.future
  }

  def getFromFileWithLock(targetFile: File): Route = {
    if (targetFile.exists()) {
      try {
        filesLocker.tryLock(targetFile, true) match {
          case Some(lock) =>
            getFromFileAndUnlock(targetFile, lock)
          case None =>
            //log.info(s"Can't lock ${targetFile} in shared mode. Retry attempt after pause")
            val delay = Promise[Unit]()
            system.scheduler.scheduleOnce(FiniteDuration(100, TimeUnit.MILLISECONDS)) {
              delay.success()
            }
            onComplete(delay.future) { _ =>
              getFromFileWithLock(targetFile)
            }
        }
      } catch {
        case ex: Exception =>
          failWith(ex)
      }
    } else {
      complete(NotFound, s"File ${targetFile} not exist")
    }
  }

  private def getFromFileAndUnlock(targetFile: File, lock: SmartFileLock): Route = {
    mapResponseEntity {
      case entity@HttpEntity.Default(contentType, contentLength, source) =>
        val future = source.runWith(Sink.fold[ByteString, ByteString](ByteString())(_ ++ _))
        future.onComplete { _ =>
          lock.release()
        }
        HttpEntity.Default(contentType, contentLength, Source.future(future))
      case entity =>
        lock.release()
        entity
    } {
      getFromFile(targetFile)
    }
  }
}
