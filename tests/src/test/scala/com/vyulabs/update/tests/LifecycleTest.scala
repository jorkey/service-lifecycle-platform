package com.vyulabs.update.tests

import com.vyulabs.update.common.version.{ClientVersion, Build}
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

class LifecycleTest extends FlatSpec with Matchers with BeforeAndAfterAll {
  behavior of "Update lifecycle"

  val lifecycle = new SimpleLifecycle("test-distribution", 8000)

  override protected def afterAll(): Unit = {
    lifecycle.close()
  }

  it should "provide simple lifecycle" in {
    lifecycle.makeAndRunDistribution("ak")
    lifecycle.installTestService(true)
    lifecycle.updateTestService()
    lifecycle.updateDistribution(ClientVersion(Build.initialBuild, 1))
  }
}
