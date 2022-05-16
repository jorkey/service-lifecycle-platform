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
    val str = new StringBuilder()
    if (service) {
      str.append(line.service)
    }
    if (instance) {
      if (str.size != 0) str += ' '
      str.append(line.instance)
    }
    if (directory) {
      if (str.size != 0) str += ' '
      str.append(line.directory)
    }
    if (process) {
      if (str.size != 0) str += ' '
      str.append(line.process)
    }
    if (task && line.task.isDefined) {
      if (str.size != 0) str += ' '
      str.append(line.task.get)
    }
    if (str.size != 0) str += ' '
    str + "%s %s %s %s".format(dateFormat.format(line.time),
      line.level, line.unit, line.message)
  }

  def serialize(line: LogLine): String = {
    "%s %s %s %s".format(dateFormat.format(line.time),
      line.level, line.unit, line.message)
  }
}
