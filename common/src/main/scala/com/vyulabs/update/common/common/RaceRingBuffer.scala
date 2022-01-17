package com.vyulabs.update.common.common

import java.io.IOException
import scala.reflect.ClassTag

class RaceRingBuffer[T](size: Int)(implicit classTag: ClassTag[T]) { self =>
  private val buffer = new Array[T](size)
  private var writeIndex = 0
  private var writeCircle = 0

  def push(v: T): Unit = {
    self.synchronized {
      buffer(writeIndex) = v
      writeIndex = incIndex(writeIndex)
      if (writeIndex == 0) {
        writeCircle += 1
      }
    }
  }

  private def incIndex(index: Int): Int = {
    val next = index + 1
    if (next == buffer.length) {
      0
    } else {
      next
    }
  }

  def makeIterator(): Iterator[T] = {
    new Iterator[T] {
      var readIndex = 0
      var readCircle = 0
      var prev = Option.empty[T]
      var value = Option.empty[T]

      self.synchronized {
        if (writeCircle != 0 || writeIndex > buffer.length/2) {
          readIndex = (writeIndex + buffer.length/2) % buffer.length
        }
        readCircle =
          if (readIndex <= writeIndex) writeCircle else writeCircle - 1
      }

      override def hasNext(): Boolean = {
        self.synchronized {
          value match {
            case Some(_) =>
              true
            case None =>
              if (writeCircle > readCircle && writeIndex >= readIndex) {
                throw new IOException("Read pointer is obsolete")
              }
              if (readIndex != writeIndex) {
                value = Some(buffer(readIndex))
                readIndex = incIndex(readIndex)
                if (readIndex == 0) {
                  readCircle += 1
                }
                true
              } else {
                false
              }
          }
        }
      }

      override def next(): T = {
        self.synchronized {
          if (hasNext()) {
            val v = value.get
            prev = value
            value = None
            v
          } else {
            throw new NoSuchElementException()
          }
        }
      }
    }
  }
}
