package com.vyulabs.update.distribution.common

import akka.stream.{Attributes, Outlet, SourceShape}
import akka.stream.stage.{AsyncCallback, GraphStageLogic, GraphStageWithMaterializedValue, OutHandler}

import scala.collection.immutable.Queue

class AkkaCallbackSource[Stream]() extends GraphStageWithMaterializedValue[SourceShape[Stream], AsyncCallback[Stream]] {
  private val out = Outlet[Stream](s"AkkaCallbackSource")

  override val shape: SourceShape[Stream] = SourceShape.of(out)

  override def createLogicAndMaterializedValue(inheritedAttributes: Attributes): (GraphStageLogic, AsyncCallback[Stream]) = {
    val logic = new GraphStageLogic(shape) {
      var buffer = Queue.empty[Stream]

      val callback = getAsyncCallback[Stream] { packet =>
        if (isAvailable(out)) {
          push(out, packet)
        } else {
          buffer = buffer.enqueue(packet)
        }
      }

      setHandler(out, new OutHandler {
        @scala.throws[Exception](classOf[Exception])
        override def onPull(): Unit = {
          for ((packet, buf) <- buffer.dequeueOption) {
            buffer = buf
            push(out, packet)
          }
        }})
    }
    (logic, logic.callback)
  }
}
