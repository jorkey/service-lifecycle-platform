package com.vyulabs.update.distribution.mongo

import akka.stream.{Attributes, FlowShape, Inlet, Outlet}
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}

/** Like Flow.takeWhile but that flow does not cancel input on complete output. */
class NippleFlow[T](condition: T => Boolean) extends GraphStage[FlowShape[T, T]] {

  private val in = Inlet[T]("NippleFlow.in")
  private val out = Outlet[T]("NippleFlow.out")

  override val shape = FlowShape.of(in, out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {
    setHandler(in, new InHandler {
      override def onPush(): Unit = {
        val elem = grab(in)
        push(out, elem)
        if (!condition(elem)) {
          complete(out)
        }
      }
    })

    setHandler(out, new OutHandler {
      override def onPull(): Unit = {
        pull(in)
      }
    })
  }
}

object NippleFlow {
  def takeWhile[T](condition: T => Boolean): NippleFlow[T] = {
    new NippleFlow[T](condition)
  }
}