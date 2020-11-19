package com.vyulabs.update.distribution.rest

import java.io.File
import java.nio.file.{Files, Paths}

import akka.http.scaladsl.model._
import akka.http.scaladsl.server.{Directives, Route}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.stream.scaladsl.FileIO
import org.scalatest.{FlatSpecLike, Matchers}

import scala.util.{Failure, Success}

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 14.01.16.
  * Copyright FanDate, Inc.
  */
class ServerTest extends FlatSpecLike with ScalatestRouteTest with Matchers with Directives {

  behavior of "Distribution"

  val route: Route =
    extractRequestContext { ctx =>
        get {
          pathPrefix("download" / ".*".r) { path =>
            val file = File.createTempFile("aaa", "txt")
            file.deleteOnExit()
            Files.write(file.toPath, "qwe123".getBytes)
            getFromFile(s"${file.getPath}")
          }
        } ~
          post {
            path("upload") {
              implicit val materializer = ctx.materializer

              mapRouteResult {
                case r =>
                  r
              } {
                fileUpload("instances-state") {
                  case (fileInfo, byteSource) =>
                    val sink = FileIO.toPath(Paths.get(s"/tmp/${fileInfo.fileName}"))
                    val future = byteSource.runWith(sink)
                    onComplete(future) {
                      case Success(_) =>
                        complete("Success")
                      case Failure(ex) =>
                        failWith(ex)
                    }
                }
              }
            }
          } ~
          head {
            path("update") {
              complete(StatusCodes.OK)
            } ~
              path("ping") {
                complete(StatusCodes.OK)
              }
          }
    }

  it should "download files" in {
    Get("/download/aaa/bbb") ~> route ~> check {
      status shouldEqual StatusCodes.OK
      responseAs[String] shouldEqual "qwe123"
    }
  }

  it should "upload files" in {
    val multipartForm =
      Multipart.FormData(Multipart.FormData.BodyPart.Strict(
        "instances-state",
        HttpEntity(ContentTypes.`application/octet-stream`, "2,3,5\n7,11,13,17,23\n29,31,37\n".getBytes),
        Map("filename" -> "primes.csv")))

    Post("/upload", multipartForm) ~> route ~> check {
      status shouldEqual StatusCodes.OK
      responseAs[String] shouldEqual "Success"
    }

    Head("/ping") ~> route ~> check {
      status shouldEqual StatusCodes.OK
    }
  }
}
