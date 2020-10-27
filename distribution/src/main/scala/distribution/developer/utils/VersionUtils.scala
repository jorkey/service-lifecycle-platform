package distribution.developer.utils

import java.util.Date

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import com.mongodb.client.model.Filters
import com.vyulabs.update.common.Common.{ClientName, ProfileName, ServiceName}
import com.vyulabs.update.distribution.developer.{DeveloperDistributionDirectory, DeveloperDistributionWebPaths}
import com.vyulabs.update.info.{ClientDesiredVersions, DesiredVersion, DesiredVersions, TestSignature, TestedVersions}
import com.vyulabs.update.utils.JsUtils.MergedJsObject
import com.vyulabs.update.version.BuildVersion
import distribution.graphql.{InvalidConfigException, NotFoundException}
import distribution.utils.{GetUtils, PutUtils}
import org.bson.BsonDocument
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success}
import spray.json._
import spray.json.DefaultJsonProtocol._

trait VersionUtils extends distribution.utils.VersionUtils
    with ClientsUtils with GetUtils with PutUtils with SprayJsonSupport {
  private implicit val log = LoggerFactory.getLogger(this.getClass)

  protected implicit val executionContext: ExecutionContext
  protected implicit val dir: DeveloperDistributionDirectory

  def setClientDesiredVersions(clientName: ClientName, desiredVersions: Seq[DesiredVersion]): Future[Boolean] = {
    log.info(s"Set client ${clientName} desired versions ${desiredVersions}")
    for {
      collection <- collections.ClientDesiredVersions
      result <- collection.replace(new BsonDocument(), ClientDesiredVersions(clientName, desiredVersions)).map(_ => true)
    } yield result
  }

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
    for {
      collection <- collections.ClientDesiredVersions
      profile <- collection.find(clientArg).map(_.headOption.map(_.versions).getOrElse(Seq.empty[DesiredVersion])
        .filter(v => serviceNames.isEmpty || serviceNames.contains(v.serviceName)))
    } yield profile
  }

  def setInstalledVersions(clientName: ClientName, desiredVersions: Seq[DesiredVersion]): Future[Boolean] = {
    for {
      collection <- collections.ClientInstalledVersions
      result <- collection.replace(new BsonDocument(), ClientDesiredVersions(clientName, desiredVersions)).map(_ => true)
    } yield result
  }

  def getInstalledVersions(clientName: ClientName): Future[Seq[DesiredVersion]] = {
    val clientArg = Filters.eq("clientName", clientName)
    for {
      collection <- collections.ClientDesiredVersions
      profile <- collection.find(clientArg).map(_.headOption.map(_.versions).getOrElse(Seq.empty[DesiredVersion]))
    } yield profile
  }

  def setTestedVersions(clientName: ClientName, desiredVersions: Seq[DesiredVersion]): Future[Boolean] = {
    for {
      clientConfig <- getClientConfig(clientName)
      testedVersions <- getTestedVersions(clientConfig.installProfile)
      result <- {
        val testRecord = TestSignature(clientName, new Date())
        val testSignatures = testedVersions match {
          case Some(testedVersions) if testedVersions.versions.equals(desiredVersions) =>
            testedVersions.signatures :+ testRecord
          case _ =>
            Seq(testRecord)
        }
        val newTestedVersions = TestedVersions(clientConfig.installProfile, desiredVersions, testSignatures)
        for {
          collection <- collections.TestedVersions
          result <- collection.replace(new BsonDocument(), newTestedVersions).map(_ => true)
        } yield result
      }
    } yield result
  }

  def getTestedVersions(profileName: ProfileName): Future[Option[TestedVersions]] = {
    val profileArg = Filters.eq("profileName", profileName)
    for {
      collection <- collections.TestedVersions
      profile <- collection.find(profileArg).map(_.headOption)
    } yield profile
  }

  def filterDesiredVersionsByProfile(clientName: ClientName, future: Future[Seq[DesiredVersion]]): Future[Seq[DesiredVersion]] = {
    for {
      desiredVersions <- future
      installProfile <- getClientInstallProfile(clientName)
      versions <- Future(desiredVersions.filter(version => installProfile.services.contains(version.serviceName)))
    } yield versions
  }

  def getMergedDesiredVersions(clientName: ClientName, serviceNames: Set[ServiceName]): Future[Seq[DesiredVersion]] = {
    for {
      clientConfig <- getClientConfig(clientName)
      developerVersions <- { clientConfig.testClientMatch match {
          case Some(testClientMatch) =>
            for {
              testedVersions <- getTestedVersions(clientConfig.installProfile).map(testedVersions => {
                testedVersions match {
                  case Some(testedVersions) =>
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
                      throw NotFoundException(s"Desired versions for profile ${clientConfig.installProfile} are not tested by clients ${testClientMatch}")
                    }
                  case None =>
                    throw NotFoundException(s"Desired versions for profile ${clientConfig.installProfile} are not tested by anyone")
                }
              })
            } yield testedVersions
          case None =>
            getDesiredVersions()
        }}.map(DesiredVersions.toMap(_))
      clientDesiredVersions <- getClientDesiredVersions(clientName, serviceNames).map(DesiredVersions.toMap(_))
      versions <- Future {
        if (clientConfig.testClientMatch.isDefined && !clientDesiredVersions.isEmpty) {
          throw InvalidConfigException("Client required preliminary testing shouldn't have personal desired versions")
        }
        val developerJson = developerVersions.toJson
        val clientJson = clientDesiredVersions.toJson
        val mergedJson = developerJson.merge(clientJson)
        val mergedVersions = mergedJson.convertTo[Map[ServiceName, BuildVersion]]
        DesiredVersions.fromMap(mergedVersions)
      }
    } yield versions
  }

  override protected def getBusyVersions(serviceName: ServiceName): Future[Set[BuildVersion]] = {
    val desiredVersion = getDesiredVersion(serviceName)
    val clientDesiredVersions = dir.getClients().map { clientName =>
      getClientDesiredVersion(clientName, serviceName, true)
    }
    val testedVersions = dir.getProfiles().map { profileName =>
      getTestedVersions(profileName).map(_.map(_.versions.find(_.serviceName == serviceName)).flatten.map(_.buildVersion))
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