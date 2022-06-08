package com.vyulabs.update.distribution.common

import akka.actor.Scheduler
import com.vyulabs.update.common.common.{Cancelable, Timer}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

class AkkaTimer(scheduler: Scheduler)(implicit executionContext: ExecutionContext) extends Timer {
  override def scheduleOnce(task: () => Unit, delay: FiniteDuration): Cancelable = {
    val cancelable = scheduler.scheduleOnce(delay, () => task())
    new Cancelable {
      override def cancel(): Boolean = cancelable.cancel()
    }
  }

  override def schedulePeriodically(task: () => Unit, period: FiniteDuration): Cancelable = {
    val cancelable = scheduler.scheduleWithFixedDelay(period, period)(() => task())
    new Cancelable {
      override def cancel(): Boolean = cancelable.cancel()
    }
  }
}
