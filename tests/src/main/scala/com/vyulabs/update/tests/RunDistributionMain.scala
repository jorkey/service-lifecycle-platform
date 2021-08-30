package com.vyulabs.update.tests

object RunDistributionMain extends App {
  println()
  println("########################### Initialize provider distribution")
  println()

  val provider = new SimpleLifecycle("provider", 8001)
  provider.makeAndRunDistribution("ak")

  println()
  println("########################### Initialize consumer distribution")
  println()
  val consumer = new SimpleLifecycle("consumer", 8000)
  consumer.makeAndRunDistributionFromProvider(provider)

  synchronized { wait() }
}
