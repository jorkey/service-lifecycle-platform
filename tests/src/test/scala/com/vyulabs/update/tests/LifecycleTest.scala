package com.vyulabs.update.tests

import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

class LifecycleTest extends FlatSpec with Matchers with BeforeAndAfterAll {
  behavior of "Update lifecycle"

  val lifecycle = new SimpleLifecycle()

  it should "provide simple lifecycle" in {
    lifecycle.makeAndRunDistribution()
    lifecycle.updateTestService()
    lifecycle.updateDistribution()
  }
}
