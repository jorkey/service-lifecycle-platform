package com.vyulabs.update.distribution.client.sync

import java.io._
import java.net.{HttpURLConnection, URL}
import java.nio.charset.StandardCharsets
import java.util.Base64

import com.vyulabs.update.distribution.DistributionWebPaths._
import com.vyulabs.update.distribution.client.graphql.GraphqlRequest
import org.slf4j.LoggerFactory
import spray.json.{JsonReader, _}

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 23.04.19.
  * Copyright FanDate, Inc.
  */
class JavaHttpClient(val distributionUrl: URL, connectTimeoutMs: Int = 1000, readTimeoutMs: Int = 1000) extends SyncHttpClient {
  implicit val log = LoggerFactory.getLogger(this.getClass)

  def graphqlRequest[Response](request: GraphqlRequest[Response])
                              (implicit reader: JsonReader[Response]): Option[Response] = {
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
      log.debug(s"Receive graphql response: ${responseJson}")
      request.decodeResponse(responseJson.asJsObject) match {
        case Left(response) =>
          Some(response)
        case Right(error) =>
          log.error(s"Decode graphql response error: ${error}")
          None
      }
    })
  }

  def upload(path: String, fieldName: String, file: File): Boolean = {
    val input =
      try {
        new FileInputStream(file)
      } catch {
        case e: IOException =>
          log.error(s"Can't open ${file}", e)
          return false
      }
    try {
      upload(path, fieldName, file.getName, input)
    } finally {
      input.close()
    }
  }

  def download(path: String, file: File): Boolean = {
    val output =
      try {
        new FileOutputStream(file)
      } catch {
        case e: IOException =>
          log.error(s"Can't open ${file}", e)
          return false
      }
    var stat = false
    try {
      stat = download(path, output)
      stat
    } finally {
      output.close()
      if (!stat) {
        file.delete()
      }
    }
  }

  def exists(path: String): Boolean = {
    executeRequest(new URL(distributionUrl.toString + "/" + path), (connection: HttpURLConnection) => {
      connection.setRequestMethod("HEAD")
      if (distributionUrl.getUserInfo != null) {
        val encoded = Base64.getEncoder.encodeToString((distributionUrl.getUserInfo).getBytes(StandardCharsets.UTF_8))
        connection.setRequestProperty("Authorization", "Basic " + encoded)
      }
      connection.setConnectTimeout(connectTimeoutMs)
      connection.setReadTimeout(readTimeoutMs)
      Some()
    }).isDefined
  }

  private def upload(path: String, fieldName: String, destinationFile: String, input: InputStream): Boolean = {
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
    }).isDefined
  }

  private def download(path: String, output: OutputStream): Boolean = {
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
    }).isDefined
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

  private final def executeRequest[T](url: URL, request: (HttpURLConnection) => Option[T]): Option[T] = {
    if (log.isDebugEnabled) log.debug(s"Make request to ${url}")
    val connection =
      try {
        url.openConnection().asInstanceOf[HttpURLConnection]
      } catch {
        case e: IOException =>
          log.error(s"Can't open connection to url ${url}, error ${e.toString}")
          return None
      }
    var result = Option.empty[T]
    try {
      result = request(connection)
    } catch {
      case e: IOException =>
        log.error(s"Error: ${e.toString}")
        None
    } finally {
      try {
        val responseCode = connection.getResponseCode
        if (responseCode != HttpURLConnection.HTTP_OK) {
          result = None
          log.error(s"Request: ${connection.getRequestMethod} ${url}")
          try {
            log.error(s"Response message: ${connection.getResponseMessage}")
          } catch {
            case _: IOException =>
          }
          try {
            val errorStream = connection.getErrorStream()
            if (errorStream != null) log.error("Response error: " + new String(errorStream.readAllBytes(), "utf8"))
          } catch {
            case _: IOException =>
          }
        }
      } catch {
        case _: IOException =>
      }
      connection.disconnect()
    }

    result
  }
}
