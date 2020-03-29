package com.vyulabs.update.config

import com.typesafe.config.Config

case class LogWriterConfig(Directory: String, FilePrefix: String, MaxFileSizeMB: Int, MaxFilesCount: Int, DateFormat: Option[String])

object LogWriterConfig {
  def apply(config: Config): LogWriterConfig = {
    val directory = config.getString("directory")
    val filePrefix = config.getString("filePrefix")
    val maxFileSizeMB = config.getInt("maxFileSizeMB")
    val maxFilesCount = config.getInt("maxFilesCount")
    val dateFormat = if (config.hasPath("dateFormat")) Some(config.getString("dateFormat")) else None
    LogWriterConfig(directory, filePrefix, maxFileSizeMB, maxFilesCount, dateFormat)
  }
}
