package com.vyulabs.update.tests

import com.vyulabs.update.common.common.ThreadTimer
import com.vyulabs.update.common.utils.Utils
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 19.02.21.
  * Copyright FanDate, Inc.
  */
object TestMain extends App {
  implicit val log = LoggerFactory.getLogger(this.getClass)
  implicit val executionContext = ExecutionContext.fromExecutor(null, ex => { ex.printStackTrace(); log.error("Uncatched exception", ex) })
  implicit val timer = new ThreadTimer()

  def usage(): String =
    "Use: <command> {[argument=value]}\n" +
    "  Commands:\n" +
    "    lifecycle"

  if (args.size < 1) Utils.error(usage())

  val command = args(0)

  command match {
    case "makeInstance" =>
      // Make distribution server
      //    BuilderMain
    case "lifecycle" =>
      new TestLifecycle().run()
      // Make distribution server
      //    BuilderMain
      // Configure service
      //    git repository, update.json
      // Make developer and client version of service
      //    graphql
      // Start updater
      //    instance.sh
      // Check for running service
      //    read file
      // Make new version of service
      //    graphql
      // Check for service is updated
      //    read file
      // Check service states
      // Check logs
    case _ =>
  }
}