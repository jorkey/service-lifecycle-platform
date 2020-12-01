package distribution.client

import java.io.File

import com.vyulabs.update.distribution.client.GraphqlRequest
import spray.json.JsonReader

import scala.concurrent.Future

trait AsyncHttpClient {
  def graphqlRequest[Response](request: GraphqlRequest[Response])(implicit reader: JsonReader[Response]): Future[Response]

  def upload(path: String, fieldName: String, file: File): Future[Unit]

  def download(path: String, file: File): Future[Unit]
}