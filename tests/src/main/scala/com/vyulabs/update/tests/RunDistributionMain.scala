package com.vyulabs.update.tests

object RunDistributionMain extends App {
  val lifecycle = new SimpleLifecycle()
  lifecycle.makeAndRunDistribution()
  lifecycle.initializeDistribution("ak")
  //lifecycle.in stallTestService()
  synchronized { wait() }
}
