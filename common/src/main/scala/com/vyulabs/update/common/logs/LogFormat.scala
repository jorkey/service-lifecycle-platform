package com.vyulabs.update.common.logs

import com.vyulabs.update.common.info.LogLine

import java.text.{ParseException, SimpleDateFormat}
import java.util.{Date, TimeZone}

object LogFormat {
  val PLAIN_OUTPUT_UNIT = "PLAIN"

  private val dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
  dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"))
  private val formattedLogRegex = "(.[^ ]* .[^ ]*) (.[^ ]*) (.[^ ]*) (.*)".r

  def parse(line: String): LogLine = {
    line match {
      case formattedLogRegex(date, level, unit, message) =>
        try {
          val logDate = dateFormat.parse(date)
          LogLine(logDate, level, unit, message, None)
        } catch {
          case _: ParseException =>
            LogLine(new Date(), "INFO", PLAIN_OUTPUT_UNIT, line, None)
        }
      case line =>
        LogLine(new Date(), "INFO", PLAIN_OUTPUT_UNIT, line, None)
    }
  }

  def serialize(line: LogLine): String = {
    "%s %s %s %s".format(dateFormat.format(line.time),
      line.level, line.unit, line.message)
  }
}
