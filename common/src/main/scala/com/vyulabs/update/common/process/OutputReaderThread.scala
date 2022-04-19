package com.vyulabs.update.common.process

import org.slf4j.Logger

import java.io.{BufferedReader, IOException}

class OutputReaderThread(input: BufferedReader, lineWaitingTimeoutMs: Option[Int],
                         terminated: () => Boolean,
                         onOutput: Seq[(String, Boolean)] => Unit,
                         onEof: () => Unit,
                         onError: (Exception) => Unit)(implicit log: Logger) extends Thread {
  override def run(): Unit = {
    val buffer = StringBuilder.newBuilder
    val chunk = new Array[Char](1024)
    try {
      var cnt = safeRead(chunk)
      while (cnt != -1) {
        var lines = Seq.empty[(String, Boolean)]
        buffer.appendAll(chunk, 0, cnt)
        var index1 = 0
        var index2 = 0
        while (index2 < buffer.size) {
          if (buffer(index2) == '\n') {
            lines :+= (buffer.substring(index1, index2), true)
            index1 = index2 + 1
          }
          index2 += 1
        }
        buffer.delete(0, index1)
        if (!buffer.isEmpty) {
          for (lineWaitingTimeoutMs <- lineWaitingTimeoutMs) {
            val expire = System.currentTimeMillis() + lineWaitingTimeoutMs
            var rest = expire - System.currentTimeMillis()
            if (rest > 0) {
              do {
                Thread.sleep(if (rest > 100) 100 else rest)
                rest = expire - System.currentTimeMillis()
              } while (!input.ready() && rest > 0)
            }
            if (!input.ready()) {
              lines :+= (buffer.toString(), false)
              buffer.clear()
            }
          }
        }
        if (!lines.isEmpty) {
          onOutput(lines)
        }
        cnt = safeRead(chunk)
      }
      onEof()
    } catch {
      case ex: Exception =>
        onError(ex)
    }
  }

  // Darwin Kernel Version 19.6.0 hangs on output reading when process is terminated.
  private def safeRead(buffer: Array[Char]): Int = {
    while (!terminated() && !input.ready()) {
      Thread.sleep(100)
    }
    try {
      input.read(buffer)
    } catch {
      case ex: IOException =>
        -1
    }
  }
}
