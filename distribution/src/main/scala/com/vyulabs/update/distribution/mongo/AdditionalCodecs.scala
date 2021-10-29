package com.vyulabs.update.distribution.mongo

import com.vyulabs.update.common.utils.JsonFormats.FiniteDurationFormat
import org.bson.codecs.{Codec, DecoderContext, EncoderContext}
import org.bson.{BsonReader, BsonWriter}
import spray.json.{enrichAny, _}

import java.net.URL
import scala.concurrent.duration.FiniteDuration

object FiniteDurationCodec extends Codec[FiniteDuration] {
  override def encode(writer: BsonWriter, value: FiniteDuration, encoderContext: EncoderContext): Unit = writer.writeString(value.toJson.toString())
  override def decode(reader: BsonReader, decoderContext: DecoderContext): FiniteDuration = reader.readString().parseJson.convertTo[FiniteDuration]
  override def getEncoderClass: Class[FiniteDuration] = classOf[FiniteDuration]
}

object URLCodec extends Codec[URL] {
  override def encode(writer: BsonWriter, value: URL, encoderContext: EncoderContext): Unit = writer.writeString(value.toString)
  override def decode(reader: BsonReader, decoderContext: DecoderContext): URL = new URL(reader.readString())
  override def getEncoderClass: Class[URL] = classOf[URL]
}
