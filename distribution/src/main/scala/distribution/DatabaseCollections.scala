package distribution

import java.io.File
import java.util.Date
import java.util.concurrent.TimeUnit

import com.mongodb.MongoClientSettings
import com.mongodb.client.model.{IndexOptions, Indexes}
import com.vyulabs.update.common.Common
import com.vyulabs.update.common.Common.InstanceId
import com.vyulabs.update.config.{ClientConfig, ClientInfo, InstallProfile}
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
    classOf[InstallProfile],
    classOf[ClientConfig],
    classOf[ClientInfo],
    classOf[ServiceState],
    classOf[ClientServiceState],
    classOf[ClientDesiredVersions],
    classOf[ClientFaultReport],
    classOf[TestedVersions],
    classOf[TestSignature],
    fromCodecs(new BuildVersionCodec())))

  val DeveloperVersionInfo = db.getOrCreateCollection[DeveloperVersionInfo]()
  val DeveloperDesiredVersions = db.getOrCreateCollection[DesiredVersions]()
  val InstalledVersionInfo = db.getOrCreateCollection[InstallVersionInfo]()
  val InstalledDesiredVersions = db.getOrCreateCollection[DesiredVersions]()

  val InstallProfile = db.getOrCreateCollection[InstallProfile]()
  val ClientInfo = db.getOrCreateCollection[ClientInfo]()
  val ClientDesiredVersions = db.getOrCreateCollection[ClientDesiredVersions]()
  val ClientInstalledDesiredVersions = db.getOrCreateCollection[ClientDesiredVersions](Some("Installed"))
  val ClientTestedVersions = db.getOrCreateCollection[TestedVersions]()
  val ClientFaultReport = db.getOrCreateCollection[ClientFaultReport]()
  val ClientServiceStates = db.getOrCreateCollection[ClientServiceState]()

  val result = for {
    developerVersionInfo <- DeveloperVersionInfo.map(
      _.createIndex(Indexes.ascending("serviceName", "clientName", "version"), new IndexOptions().unique(true)))
    installedVersionInfo <- InstalledVersionInfo.map(
      _.createIndex(Indexes.ascending("serviceName", "clientName", "version"), new IndexOptions().unique(true)))
    clientInfo <- ClientInfo
    clientInfoIndexes <- clientInfo.createIndex(Indexes.ascending("clientName"), new IndexOptions().unique(true))
    testedVersions <- ClientTestedVersions
    testedVersionsIndexes <- testedVersions.createIndex(Indexes.ascending("profileName"))
    clientDesiredVersions <- ClientDesiredVersions
    clientDesiredVersionsIndexes <- clientDesiredVersions.createIndex(Indexes.ascending("clientName"))
    clientInstalledVersions <- ClientInstalledDesiredVersions
    clientInstalledVersionsIndexes <- clientInstalledVersions.createIndex(Indexes.ascending("clientName"))
    clientFaultReport <- ClientFaultReport
    clientFaultReportIndexes <- clientFaultReport.createIndex(Indexes.ascending("clientName"))
    clientServiceStates <- ClientServiceStates
    clientServiceStatesIndexes <- {
      Future.sequence(Seq(
        clientServiceStates.createIndex(Indexes.ascending("clientName")),
        clientServiceStates.createIndex(Indexes.ascending("instanceId")),
        clientServiceStates.createIndex(Indexes.ascending("state.date"),
          new IndexOptions().expireAfter(instanceStateExpireSec, TimeUnit.SECONDS))))
    }
    stateInserts <- {
      Future.sequence(Seq(
          clientServiceStates.insert(
            ClientServiceState("distribution", instanceId,
              DirectoryServiceState.getOwnInstanceState(Common.DistributionServiceName, new Date(DistributionMain.executionStart)))),
          clientServiceStates.insert(
            ClientServiceState("distribution", instanceId,
              DirectoryServiceState.getServiceInstanceState(Common.ScriptsServiceName, new File(".")))),
        ) ++ builderDirectory.map(builderDirectory => Seq(
          clientServiceStates.insert(
            ClientServiceState("distribution", instanceId,
              DirectoryServiceState.getServiceInstanceState(Common.BuilderServiceName, new File(builderDirectory)))),
          clientServiceStates.insert(
            ClientServiceState("distribution", instanceId,
              DirectoryServiceState.getServiceInstanceState(Common.ScriptsServiceName, new File(builderDirectory)))))).getOrElse(Seq.empty)
      )
    }
  } yield (clientInfoIndexes, testedVersionsIndexes, clientDesiredVersionsIndexes, clientInstalledVersionsIndexes, clientFaultReportIndexes, clientServiceStatesIndexes,
           stateInserts)

  result.foreach(_ => log.info("Developer collections are ready"))
}
