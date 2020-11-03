package distribution

import java.io.File
import java.util.Date
import java.util.concurrent.TimeUnit

import com.mongodb.MongoClientSettings
import com.mongodb.client.model.{IndexOptions, Indexes}
import com.vyulabs.update.common.Common
import com.vyulabs.update.common.Common.InstanceId
import com.vyulabs.update.config.{ClientConfig, ClientInfo, ClientProfile}
import com.vyulabs.update.distribution.DistributionMain
import com.vyulabs.update.info._
import com.vyulabs.update.version.BuildVersion
import distribution.mongo.MongoDb
import org.bson.codecs.configuration.CodecRegistries.{fromCodecs, fromProviders, fromRegistries}
import org.bson.codecs.{Codec, DecoderContext, EncoderContext}
import org.bson.{BsonReader, BsonWriter}
import org.mongodb.scala.bson.codecs.IterableCodecProvider
import org.mongodb.scala.bson.codecs.Macros._
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

class DatabaseCollections(db: MongoDb, instanceId: InstanceId, builderDirectory: Option[String],
                          instanceStateExpireSec: Int)(implicit executionContext: ExecutionContext) {
  private implicit val log = LoggerFactory.getLogger(this.getClass)

  class BuildVersionCodec extends Codec[BuildVersion] {
    override def encode(writer: BsonWriter, value: BuildVersion, encoderContext: EncoderContext): Unit = {
      writer.writeString(value.toString)
    }

    override def decode(reader: BsonReader, decoderContext: DecoderContext): BuildVersion = {
      BuildVersion.parse(reader.readString())
    }

    override def getEncoderClass: Class[BuildVersion] = classOf[BuildVersion]
  }

  implicit def codecRegistry = fromRegistries(fromProviders(MongoClientSettings.getDefaultCodecRegistry(),
    IterableCodecProvider.apply,
    classOf[ClientInfo],
    classOf[BuildInfo],
    classOf[DeveloperVersionInfo],
    classOf[DesiredVersion],
    classOf[DesiredVersions],
    classOf[ClientProfile],
    classOf[ClientConfig],
    classOf[ClientInfo],
    classOf[ServiceState],
    classOf[ClientServiceState],
    classOf[ClientDesiredVersions],
    classOf[ClientFaultReport],
    classOf[TestedVersions],
    classOf[TestSignature],
    fromCodecs(new BuildVersionCodec())))

  val DeveloperVersionsInfo = db.getOrCreateCollection[DeveloperVersionInfo]("developer.versionsInfo")
  val DeveloperDesiredVersions = db.getOrCreateCollection[ClientDesiredVersions]("developer.desiredVersions")

  val ClientsInfo = db.getOrCreateCollection[ClientInfo]("clients.info")
  val ClientsProfiles = db.getOrCreateCollection[ClientProfile]("clients.profiles")
  val ClientsVersionsInfo = db.getOrCreateCollection[ClientVersionInfo]("clients.versionsInfo")
  val ClientsDesiredVersions = db.getOrCreateCollection[ClientDesiredVersions]("clients.desiredVersions")
  val ClientsTestedVersions = db.getOrCreateCollection[TestedVersions]("clients.testedVersions")
  val ClientsServiceStates = db.getOrCreateCollection[ClientServiceState]("clients.serviceStates")
  val ClientsFaultReports = db.getOrCreateCollection[ClientFaultReport]("clients.faultReports")

  val result = for {
    _ <- DeveloperDesiredVersions.map(_.createIndex(Indexes.ascending("clientName")))
    _ <- DeveloperVersionsInfo.map(_.createIndex(Indexes.ascending("serviceName", "clientName", "version"), new IndexOptions().unique(true)))
    _ <- ClientsInfo.map(_.createIndex(Indexes.ascending("clientName"), new IndexOptions().unique(true)))
    _ <- ClientsProfiles.map(_.createIndex(Indexes.ascending("profileName"), new IndexOptions().unique(true)))
    _ <- ClientsVersionsInfo.map(_.createIndex(Indexes.ascending("clientName", "serviceName")))
    _ <- ClientsDesiredVersions.map(_.createIndex(Indexes.ascending("clientName")))
    _ <- ClientsTestedVersions.map(_.createIndex(Indexes.ascending("profileName")))
    clientServiceStates <- ClientsServiceStates
    _ <- {
      Future.sequence(Seq(
        clientServiceStates.createIndex(Indexes.ascending("clientName")),
        clientServiceStates.createIndex(Indexes.ascending("instanceId")),
        clientServiceStates.createIndex(Indexes.ascending("state.date"),
          new IndexOptions().expireAfter(instanceStateExpireSec, TimeUnit.SECONDS))))
    }
    _ <- ClientsFaultReports.map(_.createIndex(Indexes.ascending("clientName")))

    _ <- {
      Future.sequence(Seq(
          clientServiceStates.insert(
            ClientServiceState(None, instanceId,
              DirectoryServiceState.getOwnInstanceState(Common.DistributionServiceName, new Date(DistributionMain.executionStart)))),
          clientServiceStates.insert(
            ClientServiceState(None, instanceId,
              DirectoryServiceState.getServiceInstanceState(Common.ScriptsServiceName, new File(".")))),
        ) ++ builderDirectory.map(builderDirectory => Seq(
          clientServiceStates.insert(
            ClientServiceState(None, instanceId,
              DirectoryServiceState.getServiceInstanceState(Common.BuilderServiceName, new File(builderDirectory)))),
          clientServiceStates.insert(
            ClientServiceState(None, instanceId,
              DirectoryServiceState.getServiceInstanceState(Common.ScriptsServiceName, new File(builderDirectory)))))).getOrElse(Seq.empty)
      )
    }
  } yield ()

  result.foreach(_ => log.info("Developer collections are ready"))
}
