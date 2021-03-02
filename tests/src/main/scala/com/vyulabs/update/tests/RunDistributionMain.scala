package com.vyulabs.update.tests

object RunDistributionMain extends App {
  val lifecycle = new SimpleLifecycle()
  lifecycle.makeAndRunDistribution()
  synchronized { wait() }
}
