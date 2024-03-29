package com.vyulabs.update.common.utils

import com.vyulabs.update.common.common.Common.ServiceId
import com.vyulabs.update.common.version.{ClientDistributionVersion, DeveloperDistributionVersion}
import org.slf4j.helpers.SubstituteLogger
import org.slf4j.{Logger, LoggerFactory}

import java.io.{File, IOException}
import java.text.{ParseException, SimpleDateFormat}
import java.util.jar.Attributes
import java.util.{Base64, Date, TimeZone}
import com.vyulabs.update.common.logger.TraceAppender

import scala.annotation.tailrec

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 31.07.20.
  * Copyright FanDate, Inc.
  */
object Utils {
  @tailrec
  def getLogbackLogger(cl: Class[_]): ch.qos.logback.classic.Logger = {
    val log = LoggerFactory.getLogger(cl)
    if (log.isInstanceOf[SubstituteLogger]) {
      Thread.sleep(100)
      getLogbackLogger(cl)
    } else {
      log.asInstanceOf[ch.qos.logback.classic.Logger]
    }
  }

  @tailrec
  def getLogbackLogger(name: String): ch.qos.logback.classic.Logger = {
    val log = LoggerFactory.getLogger(name)
    if (log.isInstanceOf[SubstituteLogger]) {
      Thread.sleep(100)
      getLogbackLogger(name)
    } else {
      log.asInstanceOf[ch.qos.logback.classic.Logger]
    }
  }

  def getLogbackTraceAppender(): Option[TraceAppender] = {
    val logger = getLogbackLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)
    val appender = logger.getAppender("TRACE").asInstanceOf[TraceAppender]
    if (appender != null) {
      Some(appender)
    } else {
      None
    }
  }

  def serializeISO8601Date(date: Date): String = {
    val timezone = TimeZone.getTimeZone("UTC")
    val dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    dateFormat.setTimeZone(timezone)
    dateFormat.format(date)
  }

  def parseISO8601Date(dateStr: String): Option[Date] = {
    val timezone = TimeZone.getTimeZone("UTC")
    val dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    dateFormat.setTimeZone(timezone)
    try {
      Some(dateFormat.parse(dateStr))
    } catch {
      case _: ParseException =>
        None
    }
  }

  def addCredentialsToUrl(url: String, user: String, password: String): String = {
    val index1 = url.indexOf("//")
    if (index1 != -1) {
      val index2 = if (url.indexOf('@') == -1) index1 + 2 else url.indexOf('@') + 1
      url.substring(0, index1 + 2) + user + ":" + password + "@" + url.substring(index2)
    } else {
      url
    }
  }

  def isServiceNeedUpdate(service: ServiceId,
                          ownVersion: Option[ClientDistributionVersion], desiredVersion: Option[ClientDistributionVersion])
                         (implicit log: Logger) : Option[ClientDistributionVersion] = {
    ownVersion match {
      case Some(version) =>
        desiredVersion match {
          case Some(desiredVersion) if version != desiredVersion =>
            log.info(s"Service ${service} is obsolete. Own version ${version} desired version ${desiredVersion}")
            Some(desiredVersion)
          case Some(_) =>
            log.debug(s"Service ${service} is up to date")
            None
          case None =>
            log.warn(s"No desired version for ${service}")
            None
        }
      case None =>
        None
    }
  }

  def getManifestBuildVersion(product: String)(implicit log: Logger): Option[DeveloperDistributionVersion] = {
    try {
      val resources = getClass().getClassLoader().getResources("META-INF/MANIFEST.MF")
      while (resources.hasMoreElements()) {
        val manifest = new java.util.jar.Manifest(resources.nextElement().openStream())
        val attrs = manifest.getMainAttributes()
        val titleKey = new Attributes.Name(("Implementation-Title"))
        if (attrs.containsKey(titleKey)) {
          val title = attrs.getValue(titleKey)
          if (title == product) {
            val versionKey = new Attributes.Name("Implementation-Version")
            val version = attrs.getValue(versionKey)
            return Some(DeveloperDistributionVersion.parse(version))
          }
        }
      }
      None
    } catch {
      case ex: IOException =>
        log.error("Read manifest exception", ex)
        None
    }
  }

  def extendMacro(macroString: String, args: Map[String, String], getMacroContent: String => Array[Byte])
                 (implicit log: Logger): String = {
    args.foldLeft(macroString) {
      case (m, (k, v)) =>
        val value = if (v.startsWith("%B ")) {
          val path = v.substring(3)
          Base64.getEncoder().encodeToString(getMacroContent(path))
        } else {
          v
        }
        m.replaceAll(s"%%${k}%%", value)
    }
  }

  def logException(log: Logger, message: String, ex: Throwable): Unit = {
    log.error(message, ex)
//    for (stackElement <- ex.getStackTrace) {
//      log.error(stackElement.toString)
//    }
  }

  def error(msg: String)(implicit log: Logger): Nothing = {
    log.error(msg)
    for (stackElement <- Thread.currentThread().getStackTrace) {
      log.debug(stackElement.toString)
    }
    sys.exit(1)
  }

  def restartToUpdate(msg: String)(implicit log: Logger): Nothing = {
    log.error(msg)
    sys.exit(9)
  }

  def makeDir(dir: File): File = {
    dir.mkdir(); dir
  }
}
