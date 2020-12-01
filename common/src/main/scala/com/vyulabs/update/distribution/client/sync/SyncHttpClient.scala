package com.vyulabs.update.distribution.client.sync

import java.io.File

import com.vyulabs.update.distribution.client.graphql.GraphqlRequest
import spray.json.JsonReader

/**
 * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 23.04.19.
 * Copyright FanDate, Inc.
 */
trait SyncHttpClient {
  def graphqlRequest[Response](request: GraphqlRequest[Response])(implicit reader: JsonReader[Response]): Option[Response]

  def upload(path: String, fieldName: String, file: File): Boolean

  def download(path: String, file: File): Boolean

  def exists(path: String): Boolean
}
