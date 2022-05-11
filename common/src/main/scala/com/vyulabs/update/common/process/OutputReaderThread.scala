package com.vyulabs.update.common.process

import org.slf4j.Logger

import java.io.{BufferedReader, IOException}

class OutputReaderThread(outputInput: BufferedReader,
                         errorInput: BufferedReader,
                         lineWaitingTimeoutMs: Option[Int],
                         terminated: () => Boolean,
                         onOutput: Seq[(String, Boolean)] => Unit,
                         onErrorOutput: Seq[(String, Boolean)] => Unit,
                         onEof: () => Unit,
                         onError: (Exception) => Unit)(implicit log: Logger) extends Thread {
  override def run(): Unit = {
    val outputBuffer = StringBuilder.newBuilder
    val outputChunk = new Array[Char](1024)
    val errorBuffer = StringBuilder.newBuilder
    val errorChunk = new Array[Char](1024)

    def tryGetLines(outputChunk: Array[Char], cnt: Int, buffer: StringBuilder): Seq[(String, Boolean)] = {
      var lines = Seq.empty[(String, Boolean)]
      buffer.appendAll(outputChunk, 0, cnt)
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
            } while (!outputInput.ready() && rest > 0)
          }
          if (!outputInput.ready()) {
            lines :+= (buffer.toString(), false)
            buffer.clear()
          }
        }
      }
      lines
    }

    try {
      var cnt = safeRead(outputChunk, errorChunk)
      while (cnt._1 != -1 || cnt._2 != -1) {
        if (cnt._1 > 0) {
          val lines = tryGetLines(outputChunk, cnt._1, outputBuffer)
          if (!lines.isEmpty) {
            onOutput(lines)
          }
        }
        if (cnt._2 > 0) {
          val lines = tryGetLines(errorChunk, cnt._2, errorBuffer)
          if (!lines.isEmpty) {
            onErrorOutput(lines)
          }
        }
        cnt = safeRead(outputChunk, errorChunk)
      }
      onEof()
    } catch {
      case ex: Exception =>
        onError(ex)
    }
  }

  // Darwin Kernel Version 19.6.0 hangs on output reading when process is terminated.
  private def safeRead(outBuffer: Array[Char], errBuffer: Array[Char]): (Int, Int) = {
    while (!terminated() && !outputInput.ready() && !errorInput.ready()) {
      Thread.sleep(100)
    }
    if (terminated()) {
      (-1, -1)
    } else {
      try {
        var cntOut = 0
        if (outputInput.ready()) {
          cntOut = outputInput.read(outBuffer)
        }
        var cntErr = 0
        if (errorInput.ready()) {
          cntErr = errorInput.read(errBuffer)
        }
        (cntOut, cntErr)
      } catch {
        case ex: IOException =>
          (-1, -1)
      }
    }
  }
}
