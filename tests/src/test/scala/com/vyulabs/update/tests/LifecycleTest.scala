package com.vyulabs.update.tests

import com.vyulabs.update.common.version.{ClientDistributionVersion, ClientVersion, DeveloperVersion}
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

class LifecycleTest extends FlatSpec with Matchers with BeforeAndAfterAll {
  behavior of "Update lifecycle"

  val lifecycle = new SimpleLifecycle()

  it should "provide simple lifecycle" in {
    lifecycle.makeAndRunDistribution()
    lifecycle.installTestService(true)
    lifecycle.updateTestService()
    lifecycle.updateDistribution(ClientVersion(DeveloperVersion.initialVersion, Some(1)))
  }
}
