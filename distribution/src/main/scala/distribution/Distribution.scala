package com.vyulabs.update.distribution

import java.io.{File, FileNotFoundException, IOException}
import java.nio.file.StandardOpenOption
import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes.InternalServerError
import akka.http.scaladsl.model.{HttpEntity, HttpRequest, StatusCodes}
import akka.http.scaladsl.server.Directives.{complete, failWith, fileUpload, getFromBrowseableDirectory, getFromFile}
import com.vyulabs.update.common.Common.ServiceName
import com.vyulabs.update.version.BuildVersion
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.{Credentials, FileInfo}
import akka.http.scaladsl.server.{Route, RouteResult}
import akka.stream.Materializer
import akka.stream.scaladsl.{FileIO, Sink, Source}
import akka.util.ByteString
import com.typesafe.config.{Config, ConfigParseOptions, ConfigSyntax}
import com.vyulabs.update.common.Common
import com.vyulabs.update.info.DesiredVersions
import com.vyulabs.update.users.{PasswordHash, UsersCredentials}
import com.vyulabs.update.utils.Utils
import org.slf4j.LoggerFactory

import scala.concurrent.{Future, Promise}
import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success}
import akka.pattern.after
import com.vyulabs.update.lock.{SmartFileLock, SmartFilesLocker}

import scala.concurrent.ExecutionContext.Implicits.global

class Distribution(dir: DistributionDirectory, usersCredentials: UsersCredentials)
                  (implicit system: ActorSystem, materializer: Materializer, filesLocker: SmartFilesLocker) extends DistributionWebPaths {
  private implicit val log = LoggerFactory.getLogger(this.getClass)
  private val maxVersions = 10

  protected def getDesiredVersionImage(serviceName: ServiceName, future: Future[Option[DesiredVersions]]): Route = {
    onComplete(future) {
      case Success(desiredVersions)=>
        desiredVersions match {
          case Some(desiredVersions) =>
            desiredVersions.Versions.get(serviceName) match {
              case Some(version) =>
                getFromFileWithLock(dir.getVersionImageFile(serviceName, version))
              case None =>
                complete((InternalServerError, s"No desired version for service ${serviceName}"))
            }
          case None =>
            complete((InternalServerError, s"No desired versions"))
        }
      case Failure(ex) =>
        failWith(ex)
    }
  }

  protected def getDesiredVersions(targetFile: File): Future[Option[DesiredVersions]] = {
    val promise = Promise[Option[DesiredVersions]]()
    getFileContentWithLock(targetFile).onComplete { bytes =>
      try {
        Utils.parseConfigString(bytes.get.decodeString("utf8")) match {
          case Some(config) =>
            val desiredVersions = DesiredVersions.apply(config)
            promise.success(Some(desiredVersions))
          case None =>
            promise.failure(new IOException(s"Can't parse config file ${targetFile}"))
        }
      } catch {
          case _: FileNotFoundException =>
            promise.success(None)
          case ex: Exception =>
            promise.failure(ex)
      }
    }
    promise.future
  }

  protected def getFileContentWithLock(targetFile: File)(implicit materializer: Materializer): Future[ByteString] = {
    val promise = Promise[ByteString]()
    if (targetFile.exists()) {
      try {
        filesLocker.tryLock(targetFile, true) match {
          case Some(lock) =>
            val future = FileIO.fromPath(targetFile.toPath).runWith(Sink.fold[ByteString, ByteString](ByteString())((b1, b2) => {
              b1 ++ b2
            }))
            future.onComplete { futureBytes =>
              lock.release()
              try {
                promise.success(futureBytes.get)
              } catch {
                case ex: Exception =>
                  promise.failure(ex)
              }
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
      promise.failure(new FileNotFoundException())
    }
    promise.future
  }

  protected def overwriteFileContentWithLock(targetFile: File, replaceContent: (ByteString) => Option[ByteString])
                                            (implicit materializer: Materializer): Future[Boolean] = {
    val promise = Promise[Boolean]()
    if (targetFile.exists()) {
      try {
        filesLocker.tryLock(targetFile, false) match {
          case Some(lock) =>
            val inputFuture = FileIO.fromPath(targetFile.toPath).runWith(Sink.fold[ByteString, ByteString](ByteString())(_ ++ _))
            inputFuture.onComplete { futureBytes =>
              try {
                replaceContent(futureBytes.get) match {
                  case Some(content) =>
                    val sink = FileIO.toPath(targetFile.toPath, Set(StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING))
                    val outputFuture = Source.single(content).runWith(sink)
                    outputFuture.onComplete { result =>
                      try {
                        result.get.status match {
                          case Success(_) =>
                            promise.success(true)
                          case Failure(ex) =>
                            promise.failure(ex)
                        }
                      } catch {
                        case ex: Exception =>
                          promise.failure(ex)
                      } finally {
                        lock.release()
                      }
                    }
                  case None =>
                    lock.release()
                    promise.success(false)
                }
              } catch {
                case ex: Exception =>
                  lock.release()
                  promise.failure(ex)
              }
            }
          case None =>
            log.info(s"Can't lock ${targetFile} in not shared mode. Retry attempt after pause")
            after(FiniteDuration(100, TimeUnit.MILLISECONDS), system.scheduler)(overwriteFileContentWithLock(targetFile, replaceContent))
        }
      } catch {
        case ex: Exception =>
          promise.failure(ex)
      }
    } else {
      promise.failure(new FileNotFoundException())
    }
    promise.future
  }

  protected def versionImageUpload(serviceName: ServiceName, buildVersion: BuildVersion)
                        (implicit materializer: Materializer): Route = {
    versionUpload(versionName, dir.getVersionImageFile(serviceName, buildVersion))
  }

  protected def versionInfoUpload(serviceName: ServiceName, buildVersion: BuildVersion)
                        (implicit materializer: Materializer): Route = {
    mapRouteResult {
      case result@RouteResult.Complete(_) =>
        log.info(s"Uploaded version ${buildVersion} of service ${serviceName}")
        val versionsDir = dir.getServiceDir(serviceName, buildVersion.client)
        log.info("Existing versions " + dir.getVersionsInfo(versionsDir).info.map(_.buildVersion))
        var versions = dir.getVersionsInfo(versionsDir).info.sortBy(_.date.getTime).map(_.buildVersion)
        log.info(s"Versions count is ${versions.size}")
        while (versions.size > maxVersions) {
          val lastVersion = versions.head
          log.info(s"Remove obsolete version ${lastVersion}")
          dir.removeVersion(serviceName, lastVersion)
          versions = dir.getVersionsInfo(versionsDir).info.sortBy(_.date.getTime).map(_.buildVersion)
        }
        result
      case result =>
        log.info(s"Result ${result}")
        result
    } {
      versionUpload(versionInfoName, dir.getVersionInfoFile(serviceName, buildVersion))
    }
  }

  private def versionUpload(fieldName: String, imageFile: File)(implicit materializer: Materializer): Route = {
    val directory = new File(imageFile.getParent)
    if (directory.exists() || directory.mkdir()) {
      fileUploadWithLock(fieldName, imageFile)
    } else {
      failWith(new IOException(s"Can't make directory ${directory}"))
    }
  }

  protected def getFromFileWithLock(targetFile: File)(implicit materializer: Materializer): Route = {
    if (targetFile.exists()) {
      try {
        filesLocker.tryLock(targetFile, true) match {
          case Some(lock) =>
            getFromFileAndUnlock(targetFile, lock)
          case None =>
            log.info(s"Can't lock ${targetFile} in shared mode. Retry attempt after pause")
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
      complete((InternalServerError, s"File ${targetFile} not exist"))
    }
  }

  private def getFromFileAndUnlock(targetFile: File, lock: SmartFileLock)(implicit materializer: Materializer): Route = {
    mapResponseEntity {
      case entity@HttpEntity.Default(contentType, contentLength, source) =>
        val future = source.runWith(Sink.fold[ByteString, ByteString](ByteString())(_ ++ _))
        future.onComplete { _ =>
          lock.release()
        }
        HttpEntity.Default(contentType, contentLength, Source.fromFuture(future))
      case entity =>
        lock.release()
        entity
    } {
      getFromFile(targetFile)
    }
  }

  protected def fileUploadWithLock(fieldName: String, targetFile: File)(implicit materializer: Materializer): Route = {
    fileUpload(fieldName) {
      case (fileInfo, byteSource) =>
        fileWriteWithLock(byteSource, targetFile)
    }
  }

  def fileWriteWithLock(byteSource: Source[ByteString, Any], targetFile: File, completePromise: Option[Promise[Unit]] = None)
                       (implicit materializer: Materializer): Route = {
    filesLocker.tryLock(targetFile, false) match {
      case Some(lock) =>
        val sink = FileIO.toPath(targetFile.toPath, Set(StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING))
        val result = byteSource.runWith(sink)
        onComplete(result) {
          case Success(result) =>
            lock.release()
            completePromise.foreach(_.success())
            result.status match {
              case Success(_) =>
                complete(StatusCodes.OK)
              case Failure(ex) =>
                failWith(new IOException(s"Write ${targetFile} error", ex))
            }
          case Failure(ex) =>
            failWith(ex)
        }
      case None =>
        log.info(s"Can't lock ${targetFile} in exclusively mode. Retry attempt after pause")
        val result = Source.tick(FiniteDuration(100, TimeUnit.MILLISECONDS), FiniteDuration(100, TimeUnit.MILLISECONDS), Unit)
          .take(1).runWith(Sink.ignore)
        onComplete(result) { _ =>
          fileWriteWithLock(byteSource, targetFile)
        }
    }
  }

  protected def uploadToConfig(fieldName: String, config: (Config) => Route)(implicit materializer: Materializer): Route = {
    uploadToString(fieldName, content => Utils.parseConfigString(content, ConfigParseOptions.defaults().setSyntax(ConfigSyntax.JSON)) match {
      case Some(c) =>
        config(c)
      case None =>
        failWith(new IOException(s"Can't parse config ${fieldName}"))
    })
  }

  protected def uploadToString(fieldName: String, content: (String) => Route)(implicit materializer: Materializer): Route = {
    fileUpload(fieldName) {
      case (_, byteSource) =>
        val sink = Sink.fold[ByteString, ByteString](ByteString())(_ ++ _)
        val result = byteSource.runWith(sink)
        onComplete(result) {
          case Success(result) =>
            content(result.decodeString("utf8"))
          case Failure(ex) =>
            failWith(ex)
        }
    }
  }

  protected def uploadToSource(fieldName: String, source: (FileInfo, Source[ByteString, Any]) => Route)(implicit materializer: Materializer): Route = {
    fileUpload(fieldName) {
      case (fileInfo, byteSource) =>
        source(fileInfo, byteSource)
    }
  }

  protected def getVersion(): Route = {
    Utils.getManifestBuildVersion(Common.DistributionServiceName) match {
      case Some(version) =>
        complete(version.toString)
      case None =>
        complete((InternalServerError, s"Version is not defined in manifest"))
    }
  }

  protected def browse(path: Option[String]): Route = {
    val file = path match {
      case Some(path) =>
        new File(dir.directory, path)
      case None =>
        dir.directory
    }
    if (file.isDirectory) {
      getFromBrowseableDirectory(file.getPath)
    } else {
      getFromFile(file.getPath)
    }
  }

  protected def authenticate(credentials: Credentials): Option[String] = {
    credentials match {
      case p@Credentials.Provided(user) =>
        usersCredentials.getCredentials(user) match {
          case Some(userCredentials) if p.verify(userCredentials.passwordHash.hash,
              PasswordHash.generatePasswordHash(_, userCredentials.passwordHash.salt)) =>
            Some(user)
          case _ =>
            None
        }
      case _ => None
    }
  }

  protected def requestLogger(req: HttpRequest): String = {
    "Request: " + req.toString()
  }

  protected def resultLogger(res: RouteResult): String = {
    "Result: " + res.toString()
  }
}
