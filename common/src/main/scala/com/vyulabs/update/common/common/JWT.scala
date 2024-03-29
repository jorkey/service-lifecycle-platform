package com.vyulabs.update.common.common

import com.vyulabs.update.common.info.AccessToken
import org.janjaali.sprayjwt.Jwt
import org.janjaali.sprayjwt.algorithms.HS256
import org.janjaali.sprayjwt.exceptions.InvalidSignatureException
import spray.json._
import org.slf4j.Logger

import java.io.IOException
import scala.concurrent.Future
import scala.util.{Failure, Success}

object JWT {
  def encodeAccessToken(token: AccessToken, secret: String)(implicit log: Logger): String = {
    Jwt.encode(token.toJson, secret, HS256) match {
      case Success(value) =>
        value
      case Failure(ex) =>
        throw new IOException(s"Encode access token error: ${ex.toString}")
    }
  }

  def decodeAccessToken(token: String, secret: String)(implicit log: Logger): Option[AccessToken] = {
    try {
      Jwt.decode(token, secret) match {
        case Success(jsonValue) =>
          Some(jsonValue.convertTo[AccessToken])
        case Failure(ex) =>
          log.warn(s"Decode access token ${token} error: ${ex.toString}")
          None
      }
    } catch {
      case ex: InvalidSignatureException =>
        log.warn(s"Decode access token ${token} error: ${ex.toString}")
        None
    }
  }
}
