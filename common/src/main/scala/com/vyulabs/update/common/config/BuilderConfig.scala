package com.vyulabs.update.common.config

import com.vyulabs.update.common.common.Common.{DistributionId, ServiceId}
import com.vyulabs.update.common.info.FileInfo
import spray.json.DefaultJsonProtocol
import spray.json.DefaultJsonProtocol._

import java.io.IOException

case class GitConfig(url: String, branch: String, cloneSubmodules: Option[Boolean])

object GitConfig extends DefaultJsonProtocol {
  implicit val json = jsonFormat3(GitConfig.apply)
}

case class Repository(name: String, git: GitConfig, subDirectory: Option[String])

object Repository extends DefaultJsonProtocol {
  implicit val json = jsonFormat3(Repository.apply)
}

case class NamedStringValue(name: String, value: String)

object NamedStringValue extends DefaultJsonProtocol {
  implicit val json = jsonFormat2(NamedStringValue.apply)
}

case class ServicePrivateFile(service: ServiceId, path: String) {
  def loadPath = service + "/" + path
}

object ServicePrivateFile extends DefaultJsonProtocol {
  implicit val json = jsonFormat2(ServicePrivateFile.apply)
}

case class BuildServiceConfig(service: ServiceId,
                              distribution: Option[DistributionId],
                              environment: Seq[NamedStringValue],
                              repositories: Seq[Repository],
                              privateFiles: Seq[FileInfo],
                              macroValues: Seq[NamedStringValue])

object BuildServiceConfig {
  implicit val json = jsonFormat6(BuildServiceConfig.apply)

  case class MergedBuildConfig(distribution: DistributionId,
                               environment: Seq[NamedStringValue],
                               repositories: Seq[Repository],
                               privateFiles: Seq[ServicePrivateFile],
                               macroValues: Seq[NamedStringValue])

  def merge(commonConfig: BuildServiceConfig, personalConfig: Option[BuildServiceConfig]): MergedBuildConfig = {
    val distribution = personalConfig.flatMap(_.distribution).getOrElse(commonConfig.distribution.getOrElse(
      throw new IOException("Distribution is not defined")))
    val commonEnvMap = commonConfig.environment
      .foldLeft(Map.empty[String, String])((map, value) => map + (value.name -> value.value))
    val serviceEnvMap = personalConfig.map(_.environment).getOrElse(Seq.empty)
      .foldLeft(Map.empty[String, String])((map, value) => map + (value.name -> value.value))
    val env = (commonEnvMap ++ serviceEnvMap).foldLeft(Seq.empty[NamedStringValue])((seq, entry) =>
      seq :+ NamedStringValue(entry._1, entry._2))
    val repositories = commonConfig.repositories ++ personalConfig.map(_.repositories).getOrElse(Seq.empty)
    val privateFiles =
      commonConfig.privateFiles
        .map(_.path).map(path => ServicePrivateFile(commonConfig.service, path)) ++
      personalConfig.map(_.privateFiles).getOrElse(Seq.empty)
        .map(_.path).map(path => ServicePrivateFile(personalConfig.get.service, path))
    val commonMacrosMap = commonConfig.macroValues
      .foldLeft(Map.empty[String, String])((map, value) => map + (value.name -> value.value))
    val serviceMacrosMap = personalConfig.map(_.macroValues).getOrElse(Seq.empty)
      .foldLeft(Map.empty[String, String])((map, value) => map + (value.name -> value.value))
    val macros = (commonMacrosMap ++ serviceMacrosMap).foldLeft(Seq.empty[NamedStringValue])((seq, entry) =>
      seq :+ NamedStringValue(entry._1, entry._2))
    MergedBuildConfig(distribution, env, repositories, privateFiles, macros)
  }
}