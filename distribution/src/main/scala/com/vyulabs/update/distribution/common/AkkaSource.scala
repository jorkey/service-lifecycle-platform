package com.vyulabs.update.distribution.common

import akka.actor.ActorSystem
import akka.event.Logging
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.{Attributes, Outlet, SourceShape}
import akka.stream.stage.{AsyncCallback, GraphStageLogic, GraphStageWithMaterializedValue, OutHandler}

import scala.collection.immutable.Queue

class AkkaSource[Stream]()(implicit system:ActorSystem) extends GraphStageWithMaterializedValue[SourceShape[Stream], AsyncCallback[Stream]] {
  private val log = Logging(system, getClass)
  private val out = Outlet[Stream](s"AkkaSource")

  override val shape: SourceShape[Stream] = SourceShape.of(out)

  override def createLogicAndMaterializedValue(inheritedAttributes: Attributes): (GraphStageLogic, AsyncCallback[Stream]) = {
    val logic = new GraphStageLogic(shape) {
      var buffer = Queue.empty[Stream]

      val callback = getAsyncCallback[Stream] { packet =>
          if (isAvailable(out)) {
            push(out, packet)
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

object AkkaSource {
  def ttt[T]()(implicit system:ActorSystem) = {
    val l = Source.fromGraph(new AkkaSource[T]()).runWith(Sink.asPublisher(fanout = true))
  }
}
