package com.vyulabs.update.common.settings

import java.io.File

import com.typesafe.config.{ConfigParseOptions, ConfigSyntax}
import com.vyulabs.update.common.utils.IoUtils
import org.slf4j.Logger

import scala.collection.JavaConverters._

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 04.02.19.
  * Copyright FanDate, Inc.
  */
class DefinedValues(map: Map[String, String]) {
  def propertiesExpansion(file: File)(implicit log: Logger): Boolean = {
    log.info(s"Expand macro for file ${file}")
    if (!IoUtils.macroExpansion(file, file, map)) {
      return false
    }
    true
  }
}

object DefinedValues {
  def apply(configFile: File, postValues: Map[String, String])(implicit log: Logger): Option[DefinedValues] = {
    val parseOptions = ConfigParseOptions.defaults().setSyntax(ConfigSyntax.PROPERTIES)
    val config = IoUtils.parseConfigFile(configFile, parseOptions).getOrElse(return None)
    var values = Map.empty[String, String]
    for (setting <- config.entrySet().asScala) {
      values += (setting.getKey -> setting.getValue.unwrapped().toString)
    }
    Some(new DefinedValues(values ++ postValues))
  }
}

