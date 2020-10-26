package distribution.developer.utils

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.complete
import akka.http.scaladsl.server.Route
import com.mongodb.client.model.Filters
import com.vyulabs.update.common.Common.{ClientName, ProfileName, ServiceName}
import com.vyulabs.update.distribution.developer.{DeveloperDistributionDirectory, DeveloperDistributionWebPaths}
import com.vyulabs.update.info.DesiredVersion
import com.vyulabs.update.version.BuildVersion
import distribution.utils.{GetUtils, PutUtils}
import org.bson.BsonDocument
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters.asJavaIterableConverter
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success}

trait VersionUtils extends distribution.utils.VersionUtils
    with ClientsUtils with GetUtils with PutUtils with DeveloperDistributionWebPaths with SprayJsonSupport {
  private implicit val log = LoggerFactory.getLogger(this.getClass)

  protected implicit val executionContext: ExecutionContext
  protected implicit val dir: DeveloperDistributionDirectory

  def getClientDesiredVersions(clientName: ClientName, serviceNames: Set[ServiceName], merged: Boolean): Future[Seq[DesiredVersion]] = {
    filterDesiredVersionsByProfile(clientName, if (merged) {
      getMergedDesiredVersions(clientName, serviceNames)
    } else {
      getClientDesiredVersions(clientName, serviceNames)
    })
  }

  def getClientDesiredVersion(clientName: ClientName, serviceName: ServiceName, merged: Boolean): Future[Option[BuildVersion]] = {
    getClientDesiredVersions(clientName, Set(serviceName), merged).map(_.map(_.buildVersion).headOption)
  }

  def getClientDesiredVersions(clientName: ClientName, serviceNames: Set[ServiceName] = Set.empty): Future[Seq[DesiredVersion]] = {
    val clientArg = Filters.eq("clientName", clientName)
    val serviceArgs = serviceNames.map(Filters.eq("serviceName", _))
    val args = Seq(clientArg) ++ serviceArgs
    val filters = if (!args.isEmpty) Filters.and(args.asJava) else new BsonDocument()
    for {
      collection <- collections.ClientDesiredVersion
      profile <- collection.find(filters).map(_.map(v => DesiredVersion(v.serviceName, v.buildVersion)))
    } yield profile
  }

  def getInstalledVersions(clientName: ClientName): Future[Seq[DesiredVersion]] = {
    val clientArg = Filters.eq("clientName", clientName)
    for {
      collection <- collections.ClientDesiredVersion
      profile <- collection.find(clientArg).map(_.map(v => DesiredVersion(v.serviceName, v.buildVersion)))
    } yield profile
  }

  def getTestedVersionsByProfile(profileName: ProfileName): Future[Seq[DesiredVersion]] = {
    val profileArg = Filters.eq("profileName", profileName)
    for {
      collection <- collections.TestedVersion
      profile <- collection.find(profileArg).map(_.map(v => DesiredVersion(v.serviceName, v.version)))
    } yield profile
  }

  def getTestedVersionByProfile(profileName: ProfileName, serviceName: ServiceName): Future[Option[BuildVersion]] = {
    getTestedVersionsByProfile(profileName).map(_.find(_.serviceName == serviceName).map(_.buildVersion))
  }

  def getTestedVersionsByClient(clientName: ClientName): Future[Seq[DesiredVersion]] = {
    for {
      config <- getClientConfig(clientName)
      testedVersions <- getTestedVersionsByProfile(config.installProfile)
    } yield testedVersions
  }

  def filterDesiredVersionsByProfile(clientName: ClientName, future: Future[Seq[DesiredVersion]]): Future[Seq[DesiredVersion]] = {
    for {
      desiredVersions <- future
      installProfile <- getClientInstallProfile(clientName)
      versions <- Future(desiredVersions.filter(version => installProfile.services.contains(version.serviceName)))
    } yield versions
  }

  def getMergedDesiredVersions(clientName: ClientName, serviceNames: Set[ServiceName]): Future[Seq[DesiredVersion]] = {
    /* TODO graphql
    for {
      clientConfig <- getClientConfig(clientName)
      developerVersions <- { clientConfig.testClientMatch match {
          case Some(testClientMatch) =>
            for {
              testedVersions <- getTestedVersionsByProfile(clientConfig.installProfile).map(testedVersions => {
                  val regexp = testClientMatch.r
                  val testCondition = testedVersions.signatures.exists(signature =>
                    signature.clientName match {
                      case regexp() =>
                        true
                      case _ =>
                        false
                    })
                  if (testCondition) {
                    testedVersions.versions
                  } else {
                    throw NotFoundException(s"Desired versions for client ${clientName} are not tested")
                  }
              })
            } yield testedVersions
          case None =>
            getDesiredVersions()
        }}
      clientDesiredVersions <- getClientDesiredVersions(clientName).map(DesiredVersion.toMap(_))
      versions <- Future {
        if (clientConfig.testClientMatch.isDefined && !clientDesiredVersions.isEmpty) {
          throw InvalidConfigException("Client required preliminary testing shouldn't have personal desired versions")
        }
        val developerJson = developerVersions.toJson
        val clientJson = clientDesiredVersions.map(_.toJson)
        val mergedJson = (developerJson, clientJson) match {
          case (commonJson, Some(clientJson)) =>
            commonJson.merge(clientJson)
          case (commonConfig, None) =>
            commonConfig
        }
        val mergedVersions = mergedJson.convertTo[Map[ServiceName, BuildVersion]]
        DesiredVersions.fromMap(mergedVersions)
      }
    } yield versions
     */
    Future(Seq.empty[DesiredVersion])
  }

  def uploadTestedVersions(clientName: ClientName): Route = {
    // TODO graphql
    /*uploadFileToJson(testedVersionsName, (json) => {
      val promise = Promise[Unit]()
      getClientConfig(clientName).onComplete {
        case Success(config) =>
          overwriteFileContentWithLock(dir.getTestedVersionsFile(config.installProfile), content => {
            val testedVersions =
              try { content.map(_.decodeString("utf8").parseJson.convertTo[TestedVersions]) }
              catch { case ex: Exception => log.error("Exception", ex); None }
            val versions = json.convertTo[ServicesVersions]
            val testRecord = TestSignature(clientName, new Date())
            val testSignatures = testedVersions match {
              case Some(testedVersions) =>
                if (testedVersions.versions.equals(versions.servicesVersions)) {
                  testedVersions.signatures :+ testRecord
                } else {
                  Seq(testRecord)
                }
              case None =>
                Seq(testRecord)
            }
            val newTestedVersions = TestedVersions(versions.servicesVersions, testSignatures)
            ByteString(newTestedVersions.toJson.sortedPrint.getBytes("utf8"))
          }).onComplete { promise.complete(_) }
        case Failure(e) =>
          promise.failure(e)
      }
      onSuccess(promise.future)(complete(StatusCodes.OK))
    })*/

    complete(StatusCodes.OK)
  }

  override protected def getBusyVersions(serviceName: ServiceName): Future[Set[BuildVersion]] = {
    val desiredVersion = getDesiredVersion(serviceName)
    val clientDesiredVersions = dir.getClients().map { clientName =>
      getClientDesiredVersion(clientName, serviceName, true)
    }
    val testedVersions = dir.getProfiles().map { profileName =>
      getTestedVersionByProfile(profileName, serviceName)
    }
    val promise = Promise.apply[Set[BuildVersion]]()
    Future.sequence(Set(desiredVersion) ++ clientDesiredVersions ++ testedVersions).onComplete {
      case Success(versions) =>
        promise.success(versions.flatten)
      case Failure(ex) =>
        promise.failure(ex)
    }
    promise.future
  }
}
