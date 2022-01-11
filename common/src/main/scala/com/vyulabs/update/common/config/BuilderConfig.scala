package com.vyulabs.update.common.config

import com.vyulabs.update.common.common.Common.{DistributionId, ServiceId}
import spray.json.DefaultJsonProtocol
import spray.json.DefaultJsonProtocol._

import java.io.IOException

case class GitConfig(url: String, branch: String, cloneSubmodules: Option[Boolean])

object GitConfig extends DefaultJsonProtocol {
  implicit val json = jsonFormat3(GitConfig.apply)
}

case class Repository(name: String, git: GitConfig)

object Repository extends DefaultJsonProtocol {
  implicit val json = jsonFormat2(Repository.apply)
}

case class NamedStringValue(name: String, value: String)

object NamedStringValue extends DefaultJsonProtocol {
  implicit val json = jsonFormat2(NamedStringValue.apply)
}

case class BuildServiceConfig(service: ServiceId,
                              distribution: Option[DistributionId],
                              environment: Seq[NamedStringValue],
                              repositories: Seq[Repository],
                              macroValues: Seq[NamedStringValue])

object BuildServiceConfig {
  implicit val json = jsonFormat5(BuildServiceConfig.apply)

  case class BuildConfig(distribution: DistributionId,
                         environment: Seq[NamedStringValue],
                         repositories: Seq[Repository],
                         macroValues: Seq[NamedStringValue])

  def merge(commonConfig: BuildServiceConfig, personalConfig: Option[BuildServiceConfig]): BuildConfig = {
    val distribution = personalConfig.flatMap(_.distribution).getOrElse(commonConfig.distribution.getOrElse(
      throw new IOException("Distribution is not defined")))
    val commonEnvMap = commonConfig.environment
      .foldLeft(Map.empty[String, String])((map, value) => map + (value.name -> value.value))
    val serviceEnvMap = personalConfig.map(_.environment).getOrElse(Seq.empty)
      .foldLeft(Map.empty[String, String])((map, value) => map + (value.name -> value.value))
    val env = (commonEnvMap ++ serviceEnvMap).foldLeft(Seq.empty[NamedStringValue])((seq, entry) =>
      seq :+ NamedStringValue(entry._1, entry._2))
    val repositories = commonConfig.repositories ++ personalConfig.map(_.repositories).getOrElse(Seq.empty)
    val commonMacrosMap = commonConfig.macroValues
      .foldLeft(Map.empty[String, String])((map, value) => map + (value.name -> value.value))
    val serviceMacrosMap = personalConfig.map(_.macroValues).getOrElse(Seq.empty)
      .foldLeft(Map.empty[String, String])((map, value) => map + (value.name -> value.value))
    val macros = (commonMacrosMap ++ serviceMacrosMap).foldLeft(Seq.empty[NamedStringValue])((seq, entry) =>
      seq :+ NamedStringValue(entry._1, entry._2))
    BuildConfig(distribution, env, repositories, macros)
  }
}