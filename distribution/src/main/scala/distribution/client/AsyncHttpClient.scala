package distribution.client

import java.io.File

import com.vyulabs.update.distribution.client.graphql.GraphqlRequest
import spray.json.JsonReader

import scala.concurrent.Future

/**
 * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 01.12.20.
 * Copyright FanDate, Inc.
 */
trait AsyncHttpClient {
  def graphqlRequest[Response](request: GraphqlRequest[Response])(implicit reader: JsonReader[Response]): Future[Response]

  def upload(path: String, fieldName: String, file: File): Future[Unit]

  def download(path: String, file: File): Future[Unit]
}