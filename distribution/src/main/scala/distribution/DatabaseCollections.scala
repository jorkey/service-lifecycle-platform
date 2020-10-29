package distribution

import com.mongodb.MongoClientSettings
import com.mongodb.client.model.{IndexOptions, Indexes}
import com.vyulabs.update.config.{ClientConfig, ClientInfo}
import com.vyulabs.update.info._
import distribution.mongo.MongoDb
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext
import com.vyulabs.update.version.BuildVersion
import org.bson.{BsonReader, BsonWriter}
import org.bson.codecs.{Codec, DecoderContext, EncoderContext, MapCodec}
import org.bson.codecs.configuration.CodecRegistries.{fromCodecs, fromProviders, fromRegistries}
import org.mongodb.scala.bson.codecs.Macros._

class BuildVersionCodec extends Codec[BuildVersion] {
  override def encode(writer: BsonWriter, value: BuildVersion, encoderContext: EncoderContext): Unit = {
    writer.writeString(value.toString)
  }

  override def decode(reader: BsonReader, decoderContext: DecoderContext): BuildVersion = {
    BuildVersion.parse(reader.readString())
  }

  override def getEncoderClass: Class[BuildVersion] = classOf[BuildVersion]
}


class DatabaseCollections(db: MongoDb)(implicit executionContext: ExecutionContext) {
  private implicit val log = LoggerFactory.getLogger(this.getClass)

  implicit val codecRegistry = fromRegistries(fromProviders(MongoClientSettings.getDefaultCodecRegistry(),
    classOf[BuildVersionInfo],
    classOf[VersionInfo],
    classOf[DesiredVersions]),
    fromCodecs(new BuildVersionCodec(), new MapCodec()))

  val VersionInfo = db.getOrCreateCollection[VersionInfo]()
  val DesiredVersions = db.getOrCreateCollection[DesiredVersions]()

  (for {
    versionInfoIndexes <- VersionInfo.map(
      _.createIndex(Indexes.ascending("serviceName", "clientName", "version"), new IndexOptions().unique(true)))
  } yield versionInfoIndexes)
    .foreach(_ => log.info("Common collections are ready"))
}
