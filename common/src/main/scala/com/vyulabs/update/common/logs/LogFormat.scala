package com.vyulabs.update.common.logs

import com.vyulabs.update.common.info.{LogLine, SequencedServiceLogLine}

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
          case _: NumberFormatException =>
            LogLine(new Date(), "INFO", PLAIN_OUTPUT_UNIT, line, None)
        }
      case line =>
        LogLine(new Date(), "INFO", PLAIN_OUTPUT_UNIT, line, None)
    }
  }

  def serialize(line: SequencedServiceLogLine,
                service: Boolean, instance: Boolean, directory: Boolean, process: Boolean, task: Boolean): String = {
    val str = new StringBuilder(dateFormat.format(line.time))
    if (service) {
      str.append(" " + line.service)
    }
    if (instance) {
      str.append(" " + line.instance)
    }
    if (directory) {
      str.append(" " + line.directory)
    }
    if (process) {
      str.append(" " + line.process)
    }
    if (task && line.task.isDefined) {
      str.append(" " + line.task.get)
    }
    if (!line.level.isEmpty) {
      str.append(" " + line.level)
    }
    str.append(" " + line.unit)
    str.append(" " + line.message)
    str.toString()
  }

  def serialize(line: LogLine): String = {
    "%s %s %s %s".format(dateFormat.format(line.time),
      line.level, line.unit, line.message)
  }
}
