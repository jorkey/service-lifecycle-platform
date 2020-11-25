package com.vyulabs.update.distribution

import java.net.URLEncoder

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 23.04.19.
  * Copyright FanDate, Inc.
  */
object DistributionWebPaths {

  val graphqlPathPrefix = "graphql"
  val interactiveGraphqlPathPrefix = "graphiql"
  val uiStaticPathPrefix = "ui"

  val pingPath = "ping"
  val browsePath = "browse"

  val developerVersionImagePath = "developerVersionImage"
  val clientVersionImagePath = "clientVersionImage"

  val faultReportPath = "faultReport"

  val versionImageField = "versionImage"

  def encode(pathSegment: String): String = URLEncoder.encode(pathSegment, "utf8")
}
