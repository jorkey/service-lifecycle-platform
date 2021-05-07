package com.vyulabs.update.tests

object RunDistributionMain extends App {
  val lifecycle = new SimpleLifecycle()
  lifecycle.makeAndRunDistribution()
  lifecycle.initializeDistribution("ak")
  //lif§ecycle.in stallTestService()
  synchronized { wait() }
}
