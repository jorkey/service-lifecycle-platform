package com.vyulabs.update.common.common

import org.scalatest.{FlatSpec, Matchers}
import org.slf4j.LoggerFactory

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 14.01.22.
  * Copyright FanDate, Inc.
  */
class RaceRingBufferTest extends FlatSpec with Matchers {
  behavior of "RaceRingBuffer"

  implicit val log = LoggerFactory.getLogger(this.getClass)

  it should "work as simple buffer" in {
    val buffer = new RaceRingBuffer[Int](5)
    val it = buffer.makeIterator()
    assertResult(false)(it.hasNext)
    buffer.push(1)
    buffer.push(2)
    buffer.push(3)
    assertResult(true)(it.hasNext)
    assertResult(1)(it.next())
    assertResult(true)(it.hasNext)
    assertResult(2)(it.next())
    assertResult(true)(it.hasNext)
    assertResult(3)(it.next())
    assertResult(false)(it.hasNext)
  }

  it should "work as ring buffer" in {
    val buffer = new RaceRingBuffer[Int](5)
    buffer.push(1)
    buffer.push(2)
    buffer.push(3)
    buffer.push(4)
    buffer.push(5)
    buffer.push(6)
    val it = buffer.makeIterator()
    assertResult(true)(it.hasNext)
    assertResult(4)(it.next())
    assertResult(true)(it.hasNext)
    assertResult(5)(it.next())
    assertResult(true)(it.hasNext)
    assertResult(6)(it.next())
    assertResult(false)(it.hasNext)
  }

  it should "process concurrent write and read with slow write" in {
    val buffer = new RaceRingBuffer[Int](5)
    buffer.push(2)
    buffer.push(3)
    val it = buffer.makeIterator()
    assertResult(true)(it.hasNext)
    assertResult(2)(it.next())
    assertResult(true)(it.hasNext)
    assertResult(3)(it.next())
    assertResult(false)(it.hasNext)
    buffer.push(4)
    assertResult(true)(it.hasNext)
    assertResult(4)(it.next())
    buffer.push(5)
    assertResult(true)(it.hasNext)
    assertResult(5)(it.next())
  }

  it should "process concurrent write and read with slow read" in {
    val buffer = new RaceRingBuffer[Int](5)
    buffer.push(1)
    buffer.push(2)
    val it = buffer.makeIterator()
    assertResult(true)(it.hasNext)
    assertResult(1)(it.next())
    buffer.push(3)
    buffer.push(4)
    buffer.push(5)
    buffer.push(6)
    var exception = false
    try { it.hasNext } catch {
      case _ => exception = true
    }
    assert(exception)
  }
}
