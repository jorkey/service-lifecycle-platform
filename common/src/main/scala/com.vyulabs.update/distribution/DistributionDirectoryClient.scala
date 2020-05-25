package com.vyulabs.update.distribution

import java.io._
import java.net.{HttpURLConnection, URL}
import java.nio.charset.StandardCharsets
import java.util.Base64

import com.typesafe.config.{Config, ConfigFactory, ConfigParseOptions}
import com.vyulabs.update.common.Common.ServiceName
import com.vyulabs.update.info.VersionInfo
import com.vyulabs.update.utils.Utils
import com.vyulabs.update.version.BuildVersion
import org.slf4j.Logger

import scala.annotation.tailrec

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 23.04.19.
  * Copyright FanDate, Inc.
  */
class DistributionDirectoryClient(url: URL)(implicit log: Logger) extends DistributionWebPaths {

  def isVersionExists(serviceName: ServiceName, buildVersion: BuildVersion): Boolean = {
    exists(makeUrl(getDownloadVersionPath(serviceName, buildVersion)))
  }

  def downloadVersionInfoFile(serviceName: ServiceName, buildVersion: BuildVersion, file: File): Boolean = {
     downloadToFile(makeUrl(getDownloadVersionInfoPath(serviceName, buildVersion)), file)
  }

  def downloadVersionInfo(serviceName: ServiceName, buildVersion: BuildVersion): Option[VersionInfo] = {
    downloadToConfig(makeUrl(getDownloadVersionInfoPath(serviceName, buildVersion))).map(VersionInfo(_))
  }

  def downloadVersionImage(serviceName: ServiceName, buildVersion: BuildVersion, file: File): Boolean = {
    downloadToFile(makeUrl(getDownloadVersionPath(serviceName, buildVersion)), file)
  }

  def downloadVersion(serviceName: ServiceName, buildVersion: BuildVersion, directory: File): Boolean = {
    val tmpFile = File.createTempFile(s"build", ".zip")
    try {
      if (!downloadVersionImage(serviceName, buildVersion, tmpFile)) {
        return false
      }
      if (!Utils.unzip(tmpFile, directory)) {
        log.error(s"Can't unzip version ${buildVersion} of service ${serviceName}")
        return false
      }
      true
    } finally {
      tmpFile.delete()
    }
  }

  def uploadVersionImage(serviceName: ServiceName, buildVersion: BuildVersion, infoFile: File, imageFile: File): Boolean = {
    uploadFromFile(makeUrl(getUploadVersionPath(serviceName, buildVersion)), versionName, imageFile) &&
    uploadFromFile(makeUrl(getUploadVersionInfoPath(serviceName, buildVersion)), versionInfoName, infoFile)
  }

  def uploadVersion(serviceName: ServiceName, versionInfo: VersionInfo, buildDir: File): Boolean = {
    val imageTmpFile = File.createTempFile("build", ".zip")
    val infoTmpFile = File.createTempFile("version-info", ".json")
    try {
      if (!Utils.zip(imageTmpFile, buildDir)) {
        log.error("Can't zip build directory")
        return false
      }
      if (!Utils.writeConfigFile(infoTmpFile, versionInfo.toConfig())) {
        return false
      }
      uploadVersionImage(serviceName, versionInfo.buildVersion, infoTmpFile, imageTmpFile)
    } finally {
      imageTmpFile.delete()
      infoTmpFile.delete()
    }
  }

  def waitForServerUpdated(desiredVersion: BuildVersion): Boolean = {
    log.info(s"Wait for distribution server updated to version ${desiredVersion}")
    Thread.sleep(5000)
    for (_ <- 0 until 25) {
      if (exists(makeUrl(getDistributionVersionPath))) {
        downloadToString(makeUrl(getDistributionVersionPath)) match {
          case Some(content) =>
            try {
              val version = BuildVersion.parse(content)
              if (version == desiredVersion) {
                log.info(s"Distribution server updated to version ${desiredVersion}")
                return true
              }
            } catch {
              case e: Exception =>
                log.error("Parse build version error")
                return false
            }
          case None =>
            return false
        }
      }
      Thread.sleep(1000)
    }
    log.error("Timeout of waiting for distribution server become available")
    false
  }

  def waitForServerRestarted(): Boolean = {
    log.info(s"Wait for distribution server will restarted")
    Thread.sleep(5000)
    for (_ <- 0 until 25) {
      if (exists(makeUrl(getDistributionVersionPath))) {
        downloadToString(makeUrl(getDistributionVersionPath)) match {
          case Some(content) =>
            log.info(s"Distribution server is restarted")
            return true
          case None =>
            return false
        }
      }
      Thread.sleep(1000)
    }
    log.error("Timeout of waiting for distribution server become available")
    false
  }

  protected def exists(url: URL): Boolean = {
    if (log.isDebugEnabled) log.debug(s"Check for exists ${url}")
    try {
      val resp = executeRequest(url, (connection: HttpURLConnection) => {
        if (!url.getUserInfo.isEmpty) {
          val encoded = Base64.getEncoder.encodeToString((url.getUserInfo).getBytes(StandardCharsets.UTF_8))
          connection.setRequestProperty("Authorization", "Basic " + encoded)
        }
        connection.setConnectTimeout(10000)
        connection.setReadTimeout(30000)
        connection.setRequestMethod("HEAD")
      })
      resp._1 == HttpURLConnection.HTTP_OK
    } catch {
      case e: Exception =>
        return false
      case e: StackOverflowError => // https://bugs.openjdk.java.net/browse/JDK-8214129
        log.error(s"Exception of getting head ${url}", e)
        false
    }
  }

  protected def downloadToConfig(url: URL, options: ConfigParseOptions = ConfigParseOptions.defaults()): Option[Config] = {
    downloadToString(url) match {
      case Some(entry) =>
        Utils.parseConfigString(entry, options)
      case None =>
        None
    }
  }

  protected def downloadToString(url: URL): Option[String] = {
    val output = new ByteArrayOutputStream()
    if (download(url, output)) {
      Some(output.toString("utf8"))
    } else {
      None
    }
  }

  protected def downloadToFile(url: URL, file: File): Boolean = {
    val output =
      try {
        new FileOutputStream(file)
      } catch {
        case e: IOException =>
          log.error(s"Can't open ${file}", e)
          return false
      }
    try {
      download(url, output)
    } finally {
      output.close()
    }
  }

  protected def download(url: URL, output: OutputStream): Boolean = {
    if (log.isDebugEnabled) log.debug(s"Download by url ${url}")
    try {
      val resp = executeRequest(url, (connection: HttpURLConnection) => {
        if (!url.getUserInfo.isEmpty) {
          val encoded = Base64.getEncoder.encodeToString((url.getUserInfo).getBytes(StandardCharsets.UTF_8))
          connection.setRequestProperty("Authorization", "Basic " + encoded)
        }
        connection.setConnectTimeout(10000)
        connection.setReadTimeout(30000)
        val input = connection.getInputStream
        copy(input, output)
      })
      resp._1 == HttpURLConnection.HTTP_OK
    } catch {
      case e: Exception =>
        log.error(s"Download exception from ${url}", e)
        false
    }
  }

  protected def uploadFromFile(url: URL, name: String, file: File): Boolean = {
    val input =
      try {
        new FileInputStream(file)
      } catch {
        case e: IOException =>
          log.error(s"Can't open ${file}", e)
          return false
      }
    try {
      upload(url, name, file.getName, input)
    } finally {
      input.close()
    }
  }

  protected def uploadFromString(url: URL, name: String, destinationFile: String, content: String): Boolean = {
    val input = new ByteArrayInputStream(content.getBytes("utf8"))
    upload(url, name, destinationFile, input)
  }

  protected def uploadFromConfig(url: URL, name: String, destinationFile: String, config: Config): Boolean = {
    val content = Utils.renderConfig(config, true)
    val input = new ByteArrayInputStream(content.getBytes("utf8"))
    upload(url, name, destinationFile, input)
  }

  protected def upload(url: URL, name: String, destinationFile: String, input: InputStream): Boolean = {
    if (log.isDebugEnabled) log.debug(s"Upload by url ${url}")
    val CRLF = "\r\n"
    val boundary = System.currentTimeMillis.toHexString
    try {
      val resp = executeRequest(url, (connection: HttpURLConnection) => {
        if (!url.getUserInfo.isEmpty) {
          val encoded = Base64.getEncoder.encodeToString((url.getUserInfo).getBytes(StandardCharsets.UTF_8))
          connection.setRequestProperty("Authorization", "Basic " + encoded)
        }
        connection.setConnectTimeout(10000)
        connection.setReadTimeout(30000)
        connection.setDoOutput(true)
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary)
        val output = connection.getOutputStream
        val writer = new PrintWriter(new OutputStreamWriter(output, "utf8"), true)

        writer.append("--" + boundary).append(CRLF)
        writer.append(s"Content-Type: application/octet-stream").append(CRLF)
        writer.append(f"""Content-Disposition: form-data; name="${name}"; filename="${destinationFile}"""").append(CRLF)
        writer.append(CRLF).flush

        copy(input, output)
        output.flush

        writer.append(CRLF).flush
        writer.append("--" + boundary + "--").append(CRLF).flush()
      })

      if (resp._1 != HttpURLConnection.HTTP_OK) {
        log.error(s"Uploading file ${destinationFile} response code is ${resp._1}, output ${resp._2}")
        return false
      }
      true
    } catch {
      case e: Exception =>
        log.error(s"Upload exception from ${url}", e)
        false
    }
  }

  protected def makeUrl(path: String): URL = {
    new URL(url.toString + "/" + path)
  }

  private def copy(in: InputStream, out: OutputStream): Unit = {
    val buffer = new Array[Byte](1024)
    var len = in.read(buffer)
    while (len > 0) {
      out.write(buffer, 0, len)
      len = in.read(buffer)
    }
  }

  @tailrec
  private def executeRequest(url: URL, request: (HttpURLConnection) => Unit): (Int, String) = {
    if (log.isDebugEnabled) log.debug(s"Make request to ${url}")
    val connection = url.openConnection().asInstanceOf[HttpURLConnection]
    val result = try {
      request(connection)
      (connection.getResponseCode, connection.getResponseMessage)
    } finally {
      connection.disconnect()
    }
    if (log.isDebugEnabled) log.debug(s"Response code ${result._1}, message ${result._2}")
    if (result._1 == 423) {
      if (log.isDebugEnabled) log.debug("The resource that is being accessed is locked. Retry request after pause.")
      Thread.sleep(1000)
      executeRequest(url, request)
    } else {
      result
    }
  }
}