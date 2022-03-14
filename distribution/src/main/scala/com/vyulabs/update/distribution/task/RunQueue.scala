package com.vyulabs.update.distribution.task

import scala.collection.immutable.Queue
import scala.concurrent.{ExecutionContext, Future, Promise}

class RunQueue(maxRunningCount: Int)
              (implicit executionContext: ExecutionContext) {
  private var runningCount = 0
  private var runQueue = Queue.empty[Promise[Unit]]

  def add(): (Future[Unit], Promise[Unit]) = {
    synchronized {
      val startPromise = Promise[Unit]()
      if (runningCount < maxRunningCount) {
        startPromise.success(_)
      } else {
        runQueue = runQueue.enqueue(startPromise)
      }
      val endPromise = Promise[Unit]()
      endPromise.future.foreach(_ =>
        synchronized {
          runningCount -= 1
          startWaiting()
        }
      )
      (startPromise.future, endPromise)
    }
  }

  def remove(future: Future[Unit]): Unit = {
    synchronized {
      runQueue = runQueue.filter(_ != future)
      startWaiting()
    }
  }

  private def startWaiting() = {
    synchronized {
      while (runningCount < maxRunningCount) {
        val res = runQueue.dequeue
        runQueue = res._2
        res._1.success(_)
        runningCount += 1
      }
    }
  }
}
