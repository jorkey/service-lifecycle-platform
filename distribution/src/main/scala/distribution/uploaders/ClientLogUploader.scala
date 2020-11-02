package distribution.uploaders

import java.io.{File, IOException}

import com.vyulabs.update.common.Common.InstanceId
import com.vyulabs.update.distribution.DistributionDirectory
import com.vyulabs.update.info.ProfiledServiceName
import com.vyulabs.update.log.LogWriter
import com.vyulabs.update.logs.ServiceLogs
import com.vyulabs.update.utils.IoUtils
import org.slf4j.LoggerFactory

import scala.concurrent.{Future, Promise}

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 10.12.19.
  * Copyright FanDate, Inc.
  */
class ClientLogUploader(dir: DistributionDirectory) extends Thread { self =>
  private implicit val log = LoggerFactory.getLogger(this.getClass)

  private val instanceDeadTimeout: Long = 24 * 60 * 60 * 1000

  private var writers = Map.empty[InstanceId, Map[ProfiledServiceName, LogWriter]]
  private var instancesTimestamps = Map.empty[InstanceId, Long]

  private var stopping = false

  private val directory = dir.getLogsDir()

  if (!directory.exists() && !directory.mkdir()) {
    log.error(s"Can't make directory ${directory}")
  }

  def close(): Unit = {
    self.synchronized {
      stopping = true
      notify()
    }
    join()
  }

  def receiveLogs(instanceId: InstanceId, profiledServiceName: ProfiledServiceName, serviceLogs: ServiceLogs): Future[Unit] = {
    val promise = Promise.apply[Unit]
    log.debug(s"Receive logs from instance ${instanceId} service ${profiledServiceName.toString}")
    self.synchronized {
      for (writerInit <- serviceLogs.writerInit) {
        log.info(s"Receive init of logs from instance ${instanceId} service ${profiledServiceName.toString}")
        val instanceServices = writers.get(instanceId) match {
          case Some(services) =>
            services
          case None =>
            val services = Map.empty[ProfiledServiceName, LogWriter]
            writers += (instanceId -> services)
            services
        }
        val instanceDir = new File(directory, instanceId)
        if (!instanceDir.exists() && !instanceDir.mkdir()) {
          return promise.failure(new IOException(s"Can't make directory ${instanceDir}")).future
        }
        for (writer <- instanceServices.get(profiledServiceName)) {
          log.info(s"Close log writer for service ${profiledServiceName} of instance ${instanceId}")
          writer.close()
          writers += (instanceId -> (instanceServices - profiledServiceName))
        }
        log.info(s"Open log writer for service ${profiledServiceName} of instance ${instanceId}")
        val serviceDir = new File(instanceDir, profiledServiceName.toString)
        if (!serviceDir.exists() && !serviceDir.mkdir()) {
          return promise.failure(new IOException(s"Can't make directory ${serviceDir}")).future
        }
        val writer = new LogWriter(serviceDir,
          writerInit.maxFileSizeMB * 1024 * 1024, writerInit.maxFilesCount, writerInit.filePrefix,
          ((error, exception) => log.error(error, exception)))
        writers += (instanceId -> (instanceServices + (profiledServiceName -> writer)))
      }
      writers.get(instanceId) match {
        case Some(instanceServices) =>
          val instanceDir = new File(directory, instanceId)
          if (!instanceDir.exists() && !instanceDir.mkdir()) {
            return promise.failure(new IOException(s"Can't make directory ${instanceDir}")).future
          } else {
            instancesTimestamps += (instanceId -> System.currentTimeMillis())
            instanceServices.get(profiledServiceName) match {
              case Some(writer) =>
                for (record <- serviceLogs.records) {
                  writer.writeLogLine(record, false)
                }
                writer.flush()
              case None =>
                return promise.failure(new IOException(s"Logging of service ${profiledServiceName} is not initialized")).future
            }
          }
          return promise.success().future
        case None =>
          return promise.failure(new IOException(s"Logging of instance ${instanceId} is not initialized")).future
      }
    }
  }

  override def run(): Unit = {
    log.info("Log uploader started")
    try {
      while (true) {
        self.synchronized {
          if (stopping) {
            return
          }
          wait(instanceDeadTimeout / 2)
          if (stopping) {
            return
          }
          removeDeadInstances()
        }
      }
    } catch {
      case ex: Exception =>
        log.error(s"Log uploader thread is failed", ex)
    }
  }

  private def removeDeadInstances(): Unit = {
    for (instanceDir <- directory.listFiles()) {
      val instanceId = instanceDir.getName
      val dead = instancesTimestamps.get(instanceId) match {
        case Some(timestamp) if (timestamp + instanceDeadTimeout <= System.currentTimeMillis()) =>
          instancesTimestamps -= instanceId
          true
        case None =>
          true
        case _ =>
          false
      }
      if (dead) {
        for (logs <- writers.get(instanceId)) {
          logs.foreach {
            case (_, writer) =>
              writer.close()
          }
          writers -= instanceId
        }
        if (!IoUtils.deleteFileRecursively(instanceDir)) {
          log.error(s"Can't delete ${instanceDir}")
        }
      }
    }
  }
}
