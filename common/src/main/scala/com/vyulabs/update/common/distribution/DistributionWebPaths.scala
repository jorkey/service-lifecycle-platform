package com.vyulabs.update.common.distribution

import java.net.URLEncoder

/**
 * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 23.04.19.
 * Copyright FanDate, Inc.
 */
object DistributionWebPaths {

  val graphqlPathPrefix = "graphql"
  val graphqlDslPathPrefix = "graphql-dsl"
  val uiStaticPathPrefix = "ui"

  val pingPath = "ping"

  val loadPathPrefix = "load"

  val developerVersionImagePath = "developer-version-image"
  val clientVersionImagePath = "client-version-image"

  val faultReportPath = "fault-report"

  val imageField = "image"
  val faultReportField = "fault-report"

  def encode(pathSegment: String): String = URLEncoder.encode(pathSegment, "utf8")
}
