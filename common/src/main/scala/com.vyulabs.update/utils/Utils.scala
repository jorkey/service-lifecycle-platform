package com.vyulabs.update.utils

import java.io.IOException
import java.net.{URI, URL}
import java.text.{ParseException, SimpleDateFormat}
import java.util.{Date, TimeZone}
import java.util.jar.Attributes

import com.vyulabs.update.common.Common.ServiceName
import com.vyulabs.update.version.BuildVersion
import org.slf4j.Logger
import spray.json.{JsString, JsValue, RootJsonFormat, deserializationError}

import scala.util.matching.Regex

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 31.07.20.
  * Copyright FanDate, Inc.
  */
object Utils {
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

  def isServiceNeedUpdate(serviceName: ServiceName,
                          ownVersion: Option[BuildVersion], desiredVersion: Option[BuildVersion])(implicit log: Logger): Option[BuildVersion] = {
    ownVersion match {
      case Some(version) if (!version.isEmpty()) =>
        desiredVersion match {
          case Some(desiredVersion) if !BuildVersion.ordering.equiv(version, desiredVersion) =>
            log.info(s"Service ${serviceName} is obsolete. Own version ${version} desired version ${desiredVersion}")
            Some(desiredVersion)
          case Some(_) =>
            log.debug(s"Service ${serviceName} is up to date")
            None
          case None =>
            log.warn(s"No desired version for ${serviceName}")
            None
        }
      case None =>
        None
    }
  }

  def getManifestBuildVersion(product: String)(implicit log: Logger): Option[BuildVersion] = {
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
            return Some(BuildVersion.parse(version))
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

  def extendMacro(macroString: String, args: Map[String, String]): String = {
    args.foldLeft(macroString) {
      case (m, (k, v)) => m.replaceAll(s"%%${k}%%", v)
    }
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

  object RegexJson {
    implicit object RegexFormat extends RootJsonFormat[Regex] {
      def write(value: Regex) = JsString(value.toString)
      def read(value: JsValue) = (value.asInstanceOf[JsString].value.r)
    }
  }

  object URIJson {
    implicit object URIJsonFormat extends RootJsonFormat[URI] {
      def write(value: URI) = JsString(value.toString)
      def read(value: JsValue) = new URI(value.asInstanceOf[JsString].value)
    }
  }

  object URLJson {
    implicit object URLJsonFormat extends RootJsonFormat[URL] {
      def write(value: URL) = JsString(value.toString)
      def read(value: JsValue) = new URL(value.asInstanceOf[JsString].value)
    }
  }

  object DateJson {
    implicit object DateJsonFormat extends RootJsonFormat[Date] {
      def write(value: Date) = JsString(serializeISO8601Date(value))
      def read(value: JsValue) = parseISO8601Date(value.asInstanceOf[JsString].value).getOrElse {
        deserializationError(s"Parse date error. Value is ${value}")
      }
    }
  }
}
