package com.vyulabs.update.settings

import java.io.File

import com.typesafe.config._
import com.vyulabs.update.utils.Utils
import org.slf4j.Logger

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 04.02.19.
  * Copyright FanDate, Inc.
  */
class ConfigSettings(config: Config) {
  def merge(file: File)(implicit log: Logger): Boolean = {
    if (file.exists()) {
      val origConfig = Utils.parseConfigFile(file).getOrElse(return false)
      val newConfig = config.withFallback(origConfig).resolve()
      Utils.writeConfigFile(file, newConfig)
    } else {
      Utils.writeConfigFile(file, config)
    }
  }
}