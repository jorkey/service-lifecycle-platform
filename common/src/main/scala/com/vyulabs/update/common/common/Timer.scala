package com.vyulabs.update.common.common

import java.util.TimerTask
import scala.concurrent.duration.FiniteDuration

trait Cancelable {
  def cancel(): Boolean
}

trait Timer {
  def scheduleOnce(task: () => Unit, delay: FiniteDuration): Cancelable
  def schedulePeriodically(task: () => Unit, period: FiniteDuration): Cancelable
}

class ThreadTimer() extends Timer {
  val timer = new java.util.Timer()

  override def scheduleOnce(task: () => Unit, delay: FiniteDuration): Cancelable = {
    val timerTask = new TimerTask {
      override def run(): Unit = {
        task()
      }
    }
    timer.schedule(timerTask, delay.toMillis)
    new Cancelable {
      override def cancel(): Boolean = timerTask.cancel()
    }
  }

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
