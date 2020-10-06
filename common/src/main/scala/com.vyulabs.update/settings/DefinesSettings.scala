package com.vyulabs.update.settings

import java.io.File

import com.typesafe.config.{ConfigParseOptions, ConfigSyntax}
import com.vyulabs.update.utils.IoUtils
import org.slf4j.Logger

import scala.collection.JavaConverters._

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 04.02.19.
  * Copyright FanDate, Inc.
  */
class DefinesSettings(map: Map[String, String]) {
  def propertiesExpansion(file: File)(implicit log: Logger): Boolean = {
    log.info(s"Expand macro for file ${file}")
    if (!IoUtils.macroExpansion(file, file, map)) {
      return false
    }
    true
  }
}

object DefinesSettings {
  def apply(configFile: File, preSettings: Map[String, String])(implicit log: Logger): Option[DefinesSettings] = {
    val parseOptions = ConfigParseOptions.defaults().setSyntax(ConfigSyntax.PROPERTIES)
    val config = IoUtils.parseConfigFile(configFile, parseOptions).getOrElse(return None)
    var settings = preSettings
    for (setting <- config.entrySet().asScala) {
      settings += (setting.getKey -> setting.getValue.unwrapped().toString)
    }
    Some(new DefinesSettings(settings))
  }
}

