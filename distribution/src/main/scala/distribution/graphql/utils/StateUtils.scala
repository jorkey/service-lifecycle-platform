package distribution.graphql.utils

import java.util.Date

import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.stream.Materializer
import com.mongodb.client.model.{Filters, Sorts}
import com.vyulabs.update.common.Common.{DistributionName, InstanceId, ProfileName, ServiceDirectory, ServiceName}
import com.vyulabs.update.distribution.DistributionDirectory
import com.vyulabs.update.info.{ClientDesiredVersion, DeveloperDesiredVersion, DistributionFaultReport, DistributionServiceLogLine, DistributionServiceState, InstanceServiceState, LogLine, ServiceFaultReport, ServiceLogLine, TestSignature, TestedDesiredVersions}
import distribution.config.FaultReportsConfig
import distribution.mongo.{DatabaseCollections, FaultReportDocument, InstalledDesiredVersionsDocument, MongoDbCollection, ServiceLogLineDocument, ServiceStateDocument, TestedDesiredVersionsDocument}
import org.bson.BsonDocument
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters.asJavaIterableConverter
import scala.concurrent.{ExecutionContext, Future}

trait StateUtils extends DistributionClientsUtils with SprayJsonSupport {
  private implicit val log = LoggerFactory.getLogger(this.getClass)

  protected implicit val system: ActorSystem
  protected implicit val materializer: Materializer
  protected implicit val executionContext: ExecutionContext

  protected val dir: DistributionDirectory
  protected val collections: DatabaseCollections

  protected val faultReportsConfig: FaultReportsConfig

  def setInstalledDesiredVersions(distributionName: DistributionName, desiredVersions: Seq[ClientDesiredVersion]): Future[Boolean] = {
    val clientArg = Filters.eq("distributionName", distributionName)
    for {
      collection <- collections.State_InstalledDesiredVersions
      result <- collection.replace(clientArg, InstalledDesiredVersionsDocument(distributionName, desiredVersions)).map(_ => true)
    } yield result
  }

  def getInstalledDesiredVersions(distributionName: DistributionName, serviceNames: Set[ServiceName] = Set.empty): Future[Seq[ClientDesiredVersion]] = {
    val clientArg = Filters.eq("distributionName", distributionName)
    for {
      collection <- collections.State_InstalledDesiredVersions
      profile <- collection.find(clientArg).map(_.headOption.map(_.versions).getOrElse(Seq.empty[ClientDesiredVersion]))
        .map(_.filter(v => serviceNames.isEmpty || serviceNames.contains(v.serviceName)).sortBy(_.serviceName))
    } yield profile
  }

  def getInstalledDesiredVersion(distributionName: DistributionName, serviceName: ServiceName): Future[Option[ClientDesiredVersion]] = {
    getInstalledDesiredVersions(distributionName, Set(serviceName)).map(_.headOption)
  }

  def setTestedVersions(distributionName: DistributionName, desiredVersions: Seq[DeveloperDesiredVersion]): Future[Boolean] = {
    for {
      clientConfig <- getDistributionClientConfig(distributionName)
      testedVersions <- getTestedVersions(clientConfig.installProfile)
      result <- {
        val testRecord = TestSignature(distributionName, new Date())
        val testSignatures = testedVersions match {
          case Some(testedVersions) if testedVersions.versions.equals(desiredVersions) =>
            testedVersions.signatures :+ testRecord
          case _ =>
            Seq(testRecord)
        }
        val newTestedVersions = TestedDesiredVersionsDocument(TestedDesiredVersions(clientConfig.installProfile, desiredVersions, testSignatures))
        val profileArg = Filters.eq("versions.profileName", clientConfig.installProfile)
        for {
          collection <- collections.State_TestedVersions
          result <- collection.replace(profileArg, newTestedVersions).map(_ => true)
        } yield result
      }
    } yield result
  }

  def getTestedVersions(profileName: ProfileName): Future[Option[TestedDesiredVersions]] = {
    val profileArg = Filters.eq("versions.profileName", profileName)
    for {
      collection <- collections.State_TestedVersions
      profile <- collection.find(profileArg).map(_.headOption.map(_.versions))
    } yield profile
  }

  def setServicesState(distributionName: DistributionName, instanceStates: Seq[InstanceServiceState]): Future[Boolean] = {
    for {
      collection <- collections.State_ServiceStates
      id <- collections.getNextSequence(collection.getName(), instanceStates.size)
      result <- {
        val documents = instanceStates.foldLeft(Seq.empty[ServiceStateDocument])((seq, state) => seq :+ ServiceStateDocument(
          id - (instanceStates.size - seq.size) + 1, DistributionServiceState(distributionName, state)))
        Future.sequence(documents.map(doc => {
          val filters = Filters.and(
            Filters.eq("state.distributionName", distributionName),
            Filters.eq("state.instance.serviceName", doc.state.instance.serviceName),
            Filters.eq("state.instance.instanceId", doc.state.instance.instanceId),
            Filters.eq("state.instance.directory", doc.state.instance.directory))
          collection.replace(filters, doc)
        })).map(_ => true)
      }
    } yield result
  }

  def getServicesState(distributionName: Option[DistributionName], serviceName: Option[ServiceName],
                       instanceId: Option[InstanceId], directory: Option[ServiceDirectory]): Future[Seq[DistributionServiceState]] = {
    val clientArg = distributionName.map { client => Filters.eq("state.distributionName", client) }
    val serviceArg = serviceName.map { service => Filters.eq("state.instance.serviceName", service) }
    val instanceIdArg = instanceId.map { instanceId => Filters.eq("state.instance.instanceId", instanceId) }
    val directoryArg = directory.map { directory => Filters.eq("state.instance.directory", directory) }
    val args = clientArg ++ serviceArg ++ instanceIdArg ++ directoryArg
    val filters = if (!args.isEmpty) Filters.and(args.asJava) else new BsonDocument()
    for {
      collection <- collections.State_ServiceStates
      profile <- collection.find(filters).map(_.map(_.state))
    } yield profile
  }

  def addServiceLogs(distributionName: DistributionName, serviceName: ServiceName, instanceId: InstanceId, directory: ServiceDirectory,
                     logLines: Seq[LogLine]): Future[Boolean] = {
    for {
      collection <- collections.State_ServiceLogs
      id <- collections.getNextSequence(collection.getName(), logLines.size)
      result <- collection.insert(
        logLines.foldLeft(Seq.empty[ServiceLogLineDocument])((seq, line) => { seq :+
          ServiceLogLineDocument(id - (logLines.size-seq.size) + 1,
            new DistributionServiceLogLine(distributionName, new ServiceLogLine(serviceName, instanceId, directory, line))) })).map(_ => true)
    } yield result
  }

  def addServiceFaultReportInfo(distributionName: DistributionName, report: ServiceFaultReport): Future[Boolean] = {
    for {
      collection <- collections.State_FaultReportsInfo
      id <- collections.getNextSequence(collection.getName())
      result <- collection.insert(FaultReportDocument(id, DistributionFaultReport(distributionName, report))).map(_ => true)
      _ <- clearOldReports()
    } yield result
  }

  def getDistributionFaultReportsInfo(distributionName: Option[DistributionName], serviceName: Option[ServiceName], last: Option[Int]): Future[Seq[DistributionFaultReport]] = {
    val clientArg = distributionName.map { client => Filters.eq("fault.distributionName", client) }
    val serviceArg = serviceName.map { service => Filters.eq("fault.report.info.serviceName", service) }
    val args = clientArg ++ serviceArg
    val filters = if (!args.isEmpty) Filters.and(args.asJava) else new BsonDocument()
    // https://stackoverflow.com/questions/4421207/how-to-get-the-last-n-records-in-mongodb
    val sort = last.map { last => Sorts.descending("_id") }
    for {
      collection <- collections.State_FaultReportsInfo
      faults <- collection.find(filters, sort, last).map(_.map(_.fault))
    } yield faults
  }

  private def clearOldReports(): Future[Unit] = {
    for {
      collection <- collections.State_FaultReportsInfo
      reports <- collection.find()
      result <- {
        val remainReports = reports
          .sortBy(_.fault.report.info.date)
          .filter(_.fault.report.info.date.getTime + faultReportsConfig.expirationPeriodMs >= System.currentTimeMillis())
          .takeRight(faultReportsConfig.maxFaultReportsCount)
        deleteReports(collection, reports.toSet -- remainReports.toSet)
      }
    } yield result
  }

  private def deleteReports(collection: MongoDbCollection[FaultReportDocument], reports: Set[FaultReportDocument]): Future[Unit] = {
    Future.sequence(reports.map { report =>
      log.debug(s"Delete fault report ${report._id}")
      val faultFile = dir.getFaultReportFile(report.fault.report.faultId)
      faultFile.delete()
      collection.delete(Filters.and(Filters.eq("_id", report._id)))
    }).map(_ => Unit)
  }
}
