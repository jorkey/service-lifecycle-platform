package com.vyulabs.update.distribution

import java.net.URLEncoder

import com.vyulabs.update.common.Common.ServiceName
import com.vyulabs.update.version.BuildVersion

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 23.04.19.
  * Copyright FanDate, Inc.
  */
object DistributionWebPaths {

  val graphqlPathPrefix = "graphql"
  val interactiveGraphqlPathPrefix = "graphiql"
  val uiPathPrefix = "ui"
  val pingPath = "ping"
  val browsePath = "browse"

  val downloadVersionPath = "download-version"
  val uploadVersionPath = "upload-version"
  val uploadFaultPath = "upload-service-fault"

  def downloadVersionPath(serviceName: ServiceName, version: BuildVersion): String =
    downloadVersionPath + "/" + encode(serviceName) + "/" + encode(version.toString)

  def uploadVersionPath(serviceName: ServiceName, version: BuildVersion): String =
    uploadVersionPath + "/" + encode(serviceName) + "/" + encode(version.toString)

  def uploadServiceFaultPath(serviceName: ServiceName): String =
    uploadFaultPath + "/" + encode(serviceName)

  protected def encode(pathSegment: String): String = {
    URLEncoder.encode(pathSegment, "utf8")
  }
}
