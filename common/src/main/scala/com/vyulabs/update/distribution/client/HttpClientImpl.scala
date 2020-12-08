package com.vyulabs.update.distribution.client

import com.vyulabs.update.distribution.DistributionWebPaths._
import com.vyulabs.update.distribution.client.graphql.GraphqlRequest
import org.slf4j.LoggerFactory
import spray.json.{JsonReader, _}

import java.io._
import java.net.{HttpURLConnection, URL}
import java.nio.charset.StandardCharsets
import java.util.Base64
import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 23.04.19.
  * Copyright FanDate, Inc.
  */
class HttpClientImpl(distributionUrl: URL, connectTimeoutMs: Int = 1000, readTimeoutMs: Int = 1000)
                    (implicit executionContext: ExecutionContext) extends HttpClient {
  implicit val log = LoggerFactory.getLogger(this.getClass)

  def graphql[Response](request: GraphqlRequest[Response])
                       (implicit reader: JsonReader[Response]): Future[Response] = {
    executeRequest(new URL(distributionUrl.toString + "/" + graphqlPathPrefix), (connection) => {
      if (distributionUrl.getUserInfo != null) {
        val encoded = Base64.getEncoder.encodeToString(distributionUrl.getUserInfo.getBytes(StandardCharsets.UTF_8))
        connection.setRequestProperty("Authorization", "Basic " + encoded)
      }
      connection.setRequestProperty("Content-Type", "application/json")
      connection.setRequestProperty("Accept", "application/json")
      connection.setRequestMethod("POST")
      connection.setDoOutput(true)
      val output = connection.getOutputStream
      val queryJson = request.encodeRequest()
      log.debug(s"Send graphql query: ${queryJson}")
      output.write(queryJson.compactPrint.getBytes("utf8"))
      output.flush()
      connection.setConnectTimeout(connectTimeoutMs)
      connection.setReadTimeout(readTimeoutMs)
      val input = connection.getInputStream
      val responseJson = new String(input.readAllBytes(), "utf8").parseJson
      log.debug(s"Received graphql response: ${responseJson}")
      request.decodeResponse(responseJson.asJsObject) match {
        case Left(response) =>
          response
        case Right(error) =>
          throw new IOException(error)
      }
    })
  }

  def upload(path: String, fieldName: String, file: File): Future[Unit] = {
    for {
      input <- Future {
        try {
          new FileInputStream(file)
        } catch {
          case e: IOException =>
            throw new IOException(s"Can't open ${file}", e)
        }
      }
      result <- upload(path, fieldName, file.getName, input).andThen {
        case _ => input.close()
      }
    } yield result
  }

  def download(path: String, file: File): Future[Unit] = {
    for {
      output <- Future {
        try {
          new FileOutputStream(file)
        } catch {
          case e: IOException =>
            throw new IOException(s"Can't open ${file}", e)
        }
      }
      result <- download(path, output).andThen {
        case _ => output.close()
      }
    } yield result
  }

  def exists(path: String): Future[Unit] = {
    executeRequest(new URL(distributionUrl.toString + "/" + path), (connection: HttpURLConnection) => {
      connection.setRequestMethod("HEAD")
      if (distributionUrl.getUserInfo != null) {
        val encoded = Base64.getEncoder.encodeToString((distributionUrl.getUserInfo).getBytes(StandardCharsets.UTF_8))
        connection.setRequestProperty("Authorization", "Basic " + encoded)
      }
      connection.setConnectTimeout(connectTimeoutMs)
      connection.setReadTimeout(readTimeoutMs)
    })
  }

  private def upload(path: String, fieldName: String, destinationFile: String, input: InputStream): Future[Unit] = {
    if (log.isDebugEnabled) log.debug(s"Upload by url ${path}")
    val CRLF = "\r\n"
    val boundary = System.currentTimeMillis.toHexString
    executeRequest(new URL(distributionUrl.toString + "/" + loadPathPrefix + "/" + path), (connection: HttpURLConnection) => {
      if (distributionUrl.getUserInfo != null) {
        val encoded = Base64.getEncoder.encodeToString(distributionUrl.getUserInfo.getBytes(StandardCharsets.UTF_8))
        connection.setRequestProperty("Authorization", "Basic " + encoded)
      }
      connection.setChunkedStreamingMode(0)
      connection.setConnectTimeout(connectTimeoutMs)
      connection.setReadTimeout(readTimeoutMs)
      connection.setDoOutput(true)
      connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary)
      val output = connection.getOutputStream
      val writer = new PrintWriter(new OutputStreamWriter(output, "utf8"), true)

      writer.append("--" + boundary).append(CRLF)
      writer.append(s"Content-Type: application/octet-stream").append(CRLF)
      writer.append(f"""Content-Disposition: form-data; name="${fieldName}"; filename="${destinationFile}"""").append(CRLF)
      writer.append(CRLF).flush

      copy(input, output)

      writer.append(CRLF).flush
      writer.append("--" + boundary + "--").append(CRLF).flush()
      Some()
    })
  }

  private def download(path: String, output: OutputStream): Future[Unit] = {
    if (log.isDebugEnabled) log.debug(s"Download by url ${path}")
    executeRequest(new URL(distributionUrl.toString + "/" + loadPathPrefix + "/" + path), (connection: HttpURLConnection) => {
      if (!distributionUrl.getUserInfo.isEmpty) {
        val encoded = Base64.getEncoder.encodeToString((distributionUrl.getUserInfo).getBytes(StandardCharsets.UTF_8))
        connection.setRequestProperty("Authorization", "Basic " + encoded)
      }
      connection.setConnectTimeout(connectTimeoutMs)
      connection.setReadTimeout(readTimeoutMs)
      val input = connection.getInputStream
      copy(input, output)
      Some()
    })
  }

  private def copy(in: InputStream, out: OutputStream): Unit = {
    val buffer = new Array[Byte](1024)
    var len = in.read(buffer)
    while (len > 0) {
      out.write(buffer, 0, len)
      out.flush
      len = in.read(buffer)
    }
  }

  private final def executeRequest[T](url: URL, request: (HttpURLConnection) => T): Future[T] = {
    Future {
      if (log.isDebugEnabled) log.debug(s"Make request to ${url}")
      val connection =
        try {
          url.openConnection().asInstanceOf[HttpURLConnection]
        } catch {
          case ex: IOException =>
            throw new IOException(s"Can't open connection to url ${url}, error ${ex.toString}", ex)
        }
      try {
        request(connection)
      } finally {
        try {
          val responseCode = connection.getResponseCode
          if (responseCode != HttpURLConnection.HTTP_OK) {
            val errorStream = connection.getErrorStream()
            throw new IOException(
              s"Request: ${connection.getRequestMethod} ${url}\n" +
              s"Response message: " +
                s"${ try { connection.getResponseMessage } catch { case _: Exception => "" } }\n" +
              s"Response error: " +
                new String(if (errorStream != null )
                  try { errorStream.readAllBytes() } catch { case _: Exception => Array.empty[Byte] } else Array.empty[Byte], "utf8"))
          }
        } catch {
          case _: IOException =>
        }
        connection.disconnect()
      }
    }
  }
}
