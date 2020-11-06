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

class DatabaseCollections(db: MongoDb, instanceId: InstanceId, homeDirectory: File, builderDirectory: Option[String],
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
    classOf[BuildInfo],
    classOf[DeveloperVersionInfo],
    classOf[InstalledVersionInfo],
    classOf[DesiredVersion],
    classOf[DesiredVersions],
    classOf[PersonalDesiredVersions],
    classOf[InstalledDesiredVersions],
    classOf[InstallInfo],
    classOf[ClientProfile],
    classOf[ClientConfig],
    classOf[ClientInfo],
    classOf[ServiceState],
    classOf[ClientServiceState],
    classOf[ClientServiceLogLine],
    classOf[ClientFaultReport],
    classOf[TestedDesiredVersions],
    classOf[TestSignature],
    classOf[LogLine],
    classOf[FaultInfo],
    fromCodecs(new BuildVersionCodec())))

  val Developer_VersionsInfo = db.getOrCreateCollection[DeveloperVersionInfo]("developer.versionsInfo")
  val Developer_DesiredVersions = db.getOrCreateCollection[DesiredVersions]("developer.desiredVersions")
  val Developer_PersonalDesiredVersions = db.getOrCreateCollection[PersonalDesiredVersions]("developer.personalDesiredVersions")
  val Developer_ClientsInfo = db.getOrCreateCollection[ClientInfo]("developer.clientsInfo")
  val Developer_ClientsProfiles = db.getOrCreateCollection[ClientProfile]("developer.clientsProfiles")

  val Client_VersionsInfo = db.getOrCreateCollection[InstalledVersionInfo]("client.versionsInfo")
  val Client_DesiredVersions = db.getOrCreateCollection[DesiredVersions]("client.desiredVersions")

  val State_InstalledDesiredVersions = db.getOrCreateCollection[InstalledDesiredVersions]("state.installedDesiredVersions")
  val State_TestedVersions = db.getOrCreateCollection[TestedDesiredVersions]("state.testedDesiredVersions")
  val State_ServiceStates = db.getOrCreateCollection[ClientServiceState]("state.serviceStates")
  val State_ServiceLogs = db.getOrCreateCollection[ClientServiceLogLine]("state.serviceLogs")
  val State_FaultReports = db.getOrCreateCollection[ClientFaultReport]("state.faultReports")

  val result = for {
    _ <- Developer_VersionsInfo.map(_.createIndex(Indexes.ascending("serviceName", "clientName", "version"), new IndexOptions().unique(true)))
    _ <- Developer_DesiredVersions
    _ <- Developer_PersonalDesiredVersions.map(_.createIndex(Indexes.ascending("clientName")))
    _ <- Developer_ClientsInfo.map(_.createIndex(Indexes.ascending("clientName"), new IndexOptions().unique(true)))
    _ <- Developer_ClientsProfiles.map(_.createIndex(Indexes.ascending("profileName"), new IndexOptions().unique(true)))

    _ <- Client_VersionsInfo.map(_.createIndex(Indexes.ascending("serviceName")))
    _ <- Client_DesiredVersions

    _ <- State_InstalledDesiredVersions.map(_.createIndex(Indexes.ascending("clientName")))
    _ <- State_TestedVersions.map(_.createIndex(Indexes.ascending("profileName")))
    serviceStates <- State_ServiceStates
    _ <- {
      Future.sequence(Seq(
        serviceStates.createIndex(Indexes.ascending("clientName")),
        serviceStates.createIndex(Indexes.ascending("instanceId")),
        serviceStates.createIndex(Indexes.ascending("state.date"),
          new IndexOptions().expireAfter(instanceStateExpireSec, TimeUnit.SECONDS))))
    }
    _ <- State_FaultReports.map(_.createIndex(Indexes.ascending("clientName", "faultInfo.serviceName")))

    _ <- {
      Future.sequence(Seq(
          serviceStates.insert(
            ClientServiceState(Common.OwnClient, instanceId,
              DirectoryServiceState.getServiceInstanceState(Common.DistributionServiceName, homeDirectory))),
          serviceStates.insert(
            ClientServiceState(Common.OwnClient, instanceId,
              DirectoryServiceState.getServiceInstanceState(Common.ScriptsServiceName, homeDirectory))),
        ) ++ builderDirectory.map(builderDirectory => Seq(
          serviceStates.insert(
            ClientServiceState(Common.OwnClient, instanceId,
              DirectoryServiceState.getServiceInstanceState(Common.BuilderServiceName, new File(homeDirectory, builderDirectory)))),
          serviceStates.insert(
            ClientServiceState(Common.OwnClient, instanceId,
              DirectoryServiceState.getServiceInstanceState(Common.ScriptsServiceName, new File(homeDirectory, builderDirectory)))))).getOrElse(Seq.empty)
      )
    }
  } yield ()

  result.foreach(_ => log.info("Collections are ready"))
}
