package com.vyulabs.update.common.common

import java.util.TimerTask
import scala.concurrent.duration.FiniteDuration

trait Cancelable {
  def cancel(): Boolean
}

trait Timer {
  def schedulePeriodically(task: () => Unit, period: FiniteDuration): Cancelable
}

class ThreadTimer() extends Timer {
  val timer = new java.util.Timer()

  override def schedulePeriodically(task: () => Unit, period: FiniteDuration): Cancelable = {
    val timerTask = new TimerTask {
      override def run(): Unit = {
        task()
      }
    }
    timer.schedule(timerTask, period.toMillis, period.toMillis)
    new Cancelable {
      override def cancel(): Boolean = timerTask.cancel()
    }
  }
}
