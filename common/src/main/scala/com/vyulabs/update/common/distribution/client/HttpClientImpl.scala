package com.vyulabs.update.common.distribution.client

import com.vyulabs.update.common.distribution.DistributionWebPaths._
import com.vyulabs.update.common.distribution.client.graphql.GraphqlRequest
import org.slf4j.Logger
import spray.json.{JsonReader, _}

import java.io._
import java.net.{HttpURLConnection, URL}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Failure

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 23.04.19.
  * Copyright FanDate, Inc.
  */
trait SyncSource[T] {
  def next(): Option[T]
}

class HttpClientImpl(val distributionUrl: String, initAccessToken: Option[String] = None,
                     connectTimeoutMs: Int = 1000, readTimeoutMs: Int = 1000)
                    (implicit executionContext: ExecutionContext) extends HttpClient[SyncSource] {
  accessToken = initAccessToken

  def graphql[Response](request: GraphqlRequest[Response])
                       (implicit reader: JsonReader[Response], log: Logger): Future[Response] = {
    Future {
      val connection = openConnection(graphqlPathPrefix)
      try {
//        if (distributionUrl.getAccountInfo != null) {
//          val encoded = Base64.getEncoder.encodeToString(distributionUrl.getAccountInfo.getBytes(StandardCharsets.UTF_8))
//          connection.setRequestProperty("Authorization", "Basic " + encoded)
//        }
        accessToken.foreach(token => connection.setRequestProperty("Authorization", "Bearer " + token))
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
        processResponse(connection)
        val input = connection.getInputStream
        val responseJson = new String(input.readAllBytes(), "utf8").parseJson
        log.debug(s"Received graphql response: ${responseJson}")
        request.decodeResponse(responseJson.asJsObject) match {
          case Left(response) =>
            response
          case Right(error) =>
            throw new IOException(error)
        }
      } finally {
        connection.disconnect()
      }
    }
  }

  override def subscribeSSE[Response](request: GraphqlRequest[Response])
                                     (implicit reader: JsonReader[Response], log: Logger): Future[SyncSource[Response]] = {
    Future {
      val connection = openConnection(graphqlPathPrefix)
      try {
//        if (distributionUrl.getAccountInfo != null) {
//          val encoded = Base64.getEncoder.encodeToString(distributionUrl.getAccountInfo.getBytes(StandardCharsets.UTF_8))
//          connection.setRequestProperty("Authorization", "Basic " + encoded)
//        }
        accessToken.foreach(token => connection.setRequestProperty("Authorization", "Bearer " + token))
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("Accept", "text/event-stream")
        connection.setRequestMethod("POST")
        connection.setDoOutput(true)
        val output = connection.getOutputStream
        val queryJson = request.encodeRequest()
        log.debug(s"Send graphql query: ${queryJson}")
        output.write(queryJson.compactPrint.getBytes("utf8"))
        output.flush()
        connection.setConnectTimeout(connectTimeoutMs)
        connection.setReadTimeout(readTimeoutMs)
        processResponse(connection)
        val input = connection.getInputStream
        val bufferedReader = new BufferedReader(new InputStreamReader(input))
        new SyncSource[Response] {
          override def next(): Option[Response] = {
            var line = ""
            do {
              line = bufferedReader.readLine()
            } while (line != null && line.length == 0)
            if (line != null) {
              if (line.startsWith("data:")) {
                request.decodeResponse(line.substring(5).parseJson.asJsObject) match {
                  case Left(response) =>
                    Some(response)
                  case Right(error) =>
                    throw new IOException(error)
                }
              } else {
                throw new IOException(s"Error data line: ${line}")
              }
            } else {
              connection.disconnect()
              None
            }
          }
        }
      } catch {
        case ex: Exception =>
          connection.disconnect()
          throw ex
      }
    }
  }


  override def subscribeWS[Response](request: GraphqlRequest[Response])(implicit reader: JsonReader[Response], log: Logger): Future[SyncSource[Response]] = {
    throw new UnsupportedOperationException()
  }

  def upload(path: String, fieldName: String, file: File)
            (implicit log: Logger): Future[Unit] = {
    for {
      input <- Future {
        try {
          new FileInputStream(file)
        } catch {
          case e: IOException =>
            throw new IOException(s"Can't open ${file}", e)
        }
      }
      result <- {
        val uploadFuture = upload(path, fieldName, file.getName, input)
        uploadFuture.onComplete {
          case Failure(_) => input.close()
          case _ =>
        }
        uploadFuture.map { _ => input.close() }
      }
    } yield result
  }

  def download(path: String, file: File)
              (implicit log: Logger): Future[Unit] = {
    for {
      output <- Future {
        try {
          new FileOutputStream(file)
        } catch {
          case e: IOException =>
            throw new IOException(s"Can't open ${file}", e)
        }
      }
      result <- {
        val downloadFuture = download(path, output)
        downloadFuture.onComplete {
          case Failure(_) => output.close()
          case _ =>
        }
        downloadFuture.map { _ => output.close() }
      }
    } yield result
  }

  def exists(path: String)(implicit log: Logger): Future[Unit] = {
    Future {
      val connection = openConnection(path)
      try {
        connection.setRequestMethod("HEAD")
//        if (distributionUrl.getAccountInfo != null) {
//          val encoded = Base64.getEncoder.encodeToString((distributionUrl.getAccountInfo).getBytes(StandardCharsets.UTF_8))
//          connection.setRequestProperty("Authorization", "Basic " + encoded)
//        }
        accessToken.foreach(token => connection.setRequestProperty("Authorization", "Bearer " + token))
        connection.setConnectTimeout(connectTimeoutMs)
        connection.setReadTimeout(readTimeoutMs)
        processResponse(connection)
      } finally {
        connection.disconnect()
      }
    }
  }

  private def upload(path: String, fieldName: String, destinationFile: String, input: InputStream)
                    (implicit log: Logger): Future[Unit] = {
    if (log.isDebugEnabled) log.debug(s"Upload by path ${path}")
    val CRLF = "\r\n"
    val boundary = System.currentTimeMillis.toHexString
    Future {
      val connection = openConnection(loadPathPrefix + "/" + path)
      try {
//        if (distributionUrl.getAccountInfo != null) {
//          val encoded = Base64.getEncoder.encodeToString(distributionUrl.getAccountInfo.getBytes(StandardCharsets.UTF_8))
//          connection.setRequestProperty("Authorization", "Basic " + encoded)
//        }
        accessToken.foreach(token => connection.setRequestProperty("Authorization", "Bearer " + token))
        connection.setChunkedStreamingMode(0)
        connection.setConnectTimeout(connectTimeoutMs)
//        connection.setReadTimeout(readTimeoutMs)
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
        processResponse(connection)
      } finally {
        connection.disconnect()
      }
    }
  }

  private def download(path: String, output: OutputStream)
                      (implicit log: Logger): Future[Unit] = {
    if (log.isDebugEnabled) log.debug(s"Download by url ${path}")
    Future {
      val connection = openConnection(loadPathPrefix + "/" + path)
      try {
//        if (!distributionUrl.getAccountInfo.isEmpty) {
//          val encoded = Base64.getEncoder.encodeToString((distributionUrl.getAccountInfo).getBytes(StandardCharsets.UTF_8))
//          connection.setRequestProperty("Authorization", "Basic " + encoded)
//        }
        accessToken.foreach(token => connection.setRequestProperty("Authorization", "Bearer " + token))
        connection.setConnectTimeout(connectTimeoutMs)
        connection.setReadTimeout(readTimeoutMs)
        val input = connection.getInputStream
        copy(input, output)
        processResponse(connection)
      } finally {
        connection.disconnect()
      }
    }
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

  private def openConnection(path: String)(implicit log: Logger): HttpURLConnection = {
    val url = distributionUrl + "/" + path
    if (log.isDebugEnabled) log.debug(s"Open connection ${url}")
    new URL(url).openConnection().asInstanceOf[HttpURLConnection]
  }

  private def processResponse(connection: HttpURLConnection)(implicit log: Logger): Unit = {
    val responseCode = connection.getResponseCode
//    if (log.isDebugEnabled) log.debug(s"Response code is ${responseCode}")
    if (responseCode != HttpURLConnection.HTTP_OK) {
      if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
        accessToken = None
      }
      val errorStream = connection.getErrorStream()
      throw new IOException(
        s"Request: ${connection.getRequestMethod} ${connection.getURL}\n" +
        s"Response message: " +
          s"${ try { connection.getResponseMessage } catch { case _: Exception => "" } }\n" +
        s"Response error: " +
          new String(if (errorStream != null)
            try { errorStream.readAllBytes() } catch { case _: Exception => Array.empty[Byte] } else Array.empty[Byte], "utf8"))
    }
  }
}
