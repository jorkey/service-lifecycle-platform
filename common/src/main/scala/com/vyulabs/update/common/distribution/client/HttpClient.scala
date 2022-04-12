package com.vyulabs.update.common.distribution.client

import com.vyulabs.update.common.distribution.client.graphql.GraphqlRequest
import org.slf4j.Logger
import spray.json.JsonReader

import java.io._
import scala.concurrent.Future

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 23.04.19.
  * Copyright FanDate, Inc.
  */
trait HttpClient[Stream[_]] {
  val distributionUrl: String
  @volatile var accessToken = Option.empty[String]

  def graphql[Response](request: GraphqlRequest[Response])(implicit reader: JsonReader[Response], log: Logger): Future[Response]

  def subscribeSSE[Response](request: GraphqlRequest[Response])(implicit reader: JsonReader[Response], log: Logger): Future[Stream[Response]]

  def subscribeWS[Response](request: GraphqlRequest[Response])(implicit reader: JsonReader[Response], log: Logger): Future[Stream[Response]]

  def upload(path: String, fieldName: String, file: File)(implicit log: Logger): Future[Unit]

  def download(path: String, file: File)(implicit log: Logger): Future[Unit]
}
