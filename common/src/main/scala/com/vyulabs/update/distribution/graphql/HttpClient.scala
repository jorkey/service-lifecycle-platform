package com.vyulabs.update.distribution.graphql

import com.vyulabs.update.distribution.graphql.graphql.GraphqlRequest
import spray.json.{JsonReader, _}

import java.io._
import scala.concurrent.Future

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 23.04.19.
  * Copyright FanDate, Inc.
  */
trait HttpClient {
  def graphqlRequest[Response](request: GraphqlRequest[Response])(implicit reader: JsonReader[Response]): Future[Response]

  def upload(path: String, fieldName: String, file: File): Future[Unit]

  def download(path: String, file: File): Future[Unit]

  def exists(path: String): Future[Unit]
}
