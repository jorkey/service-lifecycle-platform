package com.vyulabs.update.updater.uploaders

import com.vyulabs.update.common.Common.InstanceId
import com.vyulabs.update.common.ServiceInstanceName
import com.vyulabs.update.config.LogUploaderConfig
import com.vyulabs.update.distribution.client.ClientDistributionDirectoryClient
import com.vyulabs.update.logs.{ServiceLogs}
import org.slf4j.Logger

import scala.collection.immutable.Queue

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 9.12.19.
  * Copyright FanDate, Inc.
  */
class LogUploader(instanceId: InstanceId, serviceInstanceName: ServiceInstanceName, logUploaderConfig: LogUploaderConfig,
                  clientDirectory: ClientDistributionDirectoryClient)(implicit log: Logger) extends Thread { self =>

  private val notifyThreshold = 50
  private var queue = Queue.empty[String]
  private var stopping = false

  def writeLogLine(line: String): Unit = {
    self.synchronized {
      queue = queue.enqueue(line)
      if (queue.size == notifyThreshold) {
        self.synchronized {
          self.notify()
        }
      }
    }
  }

  def close(): Unit = {
    self.synchronized {
      stopping = true
      notify()
    }
    join()
  }

  override def run(): Unit = {
    var mustStop = false
    while (!mustStop) {
      try {
        val logs = self.synchronized {
          self.wait(10000)
          mustStop = stopping
          val logs = queue
          queue = Queue.empty
          logs
        }
        if (!clientDirectory.uploadServiceLogs(instanceId, serviceInstanceName, new ServiceLogs(None, logs))) {
          log.debug("Upload of service logs is failed - resend with writer init")
          clientDirectory.uploadServiceLogs(instanceId, serviceInstanceName, new ServiceLogs(Some(logUploaderConfig.writer), logs))
        }
      } catch {
        case e: Exception =>
          log.error(s"Uploading of service ${serviceInstanceName} logs is failed", e)
      }
    }
  }
}
