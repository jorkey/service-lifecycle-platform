package com.vyulabs.update.updater

import java.io.{BufferedReader, InputStream, InputStreamReader}

import org.slf4j.Logger

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 8.05.19.
  * Copyright FanDate, Inc.
  */
class ReaderThread(state: ServiceStateController, process: Process,
                   onLine: String => Unit, onEof: () => Unit)(implicit log: Logger) extends Thread {
  val input = new BufferedReader(new InputStreamReader(process.getInputStream))

  override def run(): Unit = {
    try {
      var line = input.readLine
      while (line != null) {
        onLine(line)
        line = input.readLine
      }
    } catch {
      case e: Exception =>
        if (process.isAlive) {
          state.error(s"Read service output error ${e.getMessage}")
        }
    }
    onEof()
  }
}
