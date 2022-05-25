package com.vyulabs.update.common.utils

import spray.json.{DeserializationException, JsNumber, JsObject, JsString, JsValue, RootJsonFormat}

import java.net.{URI, URL}
import java.util.Date
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration
import scala.util.matching.Regex

object JsonFormats {
  implicit object RegexFormat extends RootJsonFormat[Regex] {
    def write(value: Regex) = JsString(value.toString)
    def read(value: JsValue) = (value.asInstanceOf[JsString].value.r)
  }

  implicit object URIJsonFormat extends RootJsonFormat[URI] {
    def write(value: URI) = JsString(value.toString)
    def read(value: JsValue) = new URI(value.asInstanceOf[JsString].value)
  }

  implicit object URLJsonFormat extends RootJsonFormat[URL] {
    def write(value: URL) = JsString(value.toString)
    def read(value: JsValue) = new URL(value.asInstanceOf[JsString].value)
  }

  implicit object DateJsonFormat extends RootJsonFormat[Date] {
    def write(value: Date) = JsNumber(value.getTime)
    def read(value: JsValue) = new Date(value.asInstanceOf[JsNumber].value.longValue())
  }

  implicit object FiniteDurationFormat extends RootJsonFormat[FiniteDuration] {
    def write(fd: FiniteDuration) = JsObject(
      "length" -> JsNumber(fd.length),
      "unit" -> JsString(fd.unit.name())
    )

    def read(value: JsValue) = {
      value.asJsObject.getFields("length", "unit") match {
        case Seq(JsNumber(length), JsString(unit)) =>
          FiniteDuration(length.toLong, TimeUnit.valueOf(unit))
        case _ => throw new DeserializationException("FiniteDuration expected")
      }
    }
  }
}
