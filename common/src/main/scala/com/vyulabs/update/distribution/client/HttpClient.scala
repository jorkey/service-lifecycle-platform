package com.vyulabs.update.distribution.client

import java.io._
import java.net.{HttpURLConnection, URL}
import java.nio.charset.StandardCharsets
import java.util.Base64

import org.slf4j.Logger
import spray.json.{JsValue, JsonReader}

import spray.json.DefaultJsonProtocol._
import spray.json._

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 23.04.19.
  * Copyright FanDate, Inc.
  */
class HttpClient(val url: URL, connectTimeoutMs: Int, readTimeoutMs: Int)(implicit log: Logger) {
  def graphqlRequest[T](request: String, command: String, arguments: Map[String, JsValue] = Map.empty)
                       (implicit reader: JsonReader[T]): Option[T]= {
    executeRequest(makeUrl("graphql"), (connection) => {
      connection.setRequestMethod("POST")
      connection.setRequestProperty("Content-Type", "application/json")
      val output = connection.getOutputStream
      output.write(s"${request} { ${command} ( ${arguments.toJson} ) }".getBytes("utf8"))
      output.flush()
      connection.setConnectTimeout(connectTimeoutMs)
      connection.setReadTimeout(readTimeoutMs)
      val input = connection.getInputStream
      val response = new String(input.readAllBytes(), "utf8").parseJson
      val fields = response.asJsObject.fields
      fields.get("data") match {
        case Some(data) if (data != JsNull) =>
          val response = data.asJsObject.fields.get(request).getOrElse(throw new IOException())
          Some(response.convertTo[T])
        case _ =>
          fields.get("errors") match {
            case Some(errors) =>
              log.error(s"Graphql request error: ${errors}")
            case None =>
              log.error(s"Graphql invalid response: ${response}")
          }
          None
      }
    })
  }

  def upload(url: URL, name: String, destinationFile: String, input: InputStream): Boolean = {
    if (log.isDebugEnabled) log.debug(s"Upload by url ${url}")
    val CRLF = "\r\n"
    val boundary = System.currentTimeMillis.toHexString
    executeRequest(url, (connection: HttpURLConnection) => {
      if (!url.getUserInfo.isEmpty) {
        val encoded = Base64.getEncoder.encodeToString(url.getUserInfo.getBytes(StandardCharsets.UTF_8))
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
      writer.append(f"""Content-Disposition: form-data; name="${name}"; filename="${destinationFile}"""").append(CRLF)
      writer.append(CRLF).flush

      copy(input, output)

      writer.append(CRLF).flush
      writer.append("--" + boundary + "--").append(CRLF).flush()
      Some()
    }).isDefined
  }

  def download(url: URL, output: OutputStream): Boolean = {
    if (log.isDebugEnabled) log.debug(s"Download by url ${url}")
    executeRequest(url, (connection: HttpURLConnection) => {
      if (!url.getUserInfo.isEmpty) {
        val encoded = Base64.getEncoder.encodeToString((url.getUserInfo).getBytes(StandardCharsets.UTF_8))
        connection.setRequestProperty("Authorization", "Basic " + encoded)
      }
      connection.setConnectTimeout(connectTimeoutMs)
      connection.setReadTimeout(readTimeoutMs)
      val input = connection.getInputStream
      copy(input, output)
      Some()
    }).isDefined
  }

  def copy(in: InputStream, out: OutputStream): Unit = {
    val buffer = new Array[Byte](1024)
    var len = in.read(buffer)
    while (len > 0) {
      out.write(buffer, 0, len)
      out.flush
      len = in.read(buffer)
    }
  }

  def makeUrl(path: String): URL = {
    new URL(url.toString + "/" + path)
  }

  private final def executeRequest[T](url: URL, request: (HttpURLConnection) => Option[T]): Option[T] = {
    if (log.isDebugEnabled) log.debug(s"Make request to ${url}")
    val connection =
      try {
        url.openConnection().asInstanceOf[HttpURLConnection]
      } catch {
        case e: IOException =>
          log.error(s"Can't open connection to URL ${url}, error ${e.getMessage}")
          return None
      }
    try {
      request(connection)
    } catch {
      case e: IOException =>
        log.error(s"Error: ${e.getMessage}")
        None
    } finally {
      try {
        val responseCode = connection.getResponseCode
        if (responseCode != HttpURLConnection.HTTP_OK) {
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
  }
}
