package com.vyulabs.update.builder.config

import java.io.File
import java.net.URI

import com.typesafe.config.{Config}
import com.vyulabs.update.common.Common.ServiceName
import com.vyulabs.update.utils.IOUtils
import org.slf4j.Logger

import scala.collection.JavaConverters._

case class SourceRepository(serviceName: ServiceName, sourceRepositories: Seq[RepositoryConfig])

object SourceRepository {
  def apply(config: Config): SourceRepository = {
    val serviceName = config.getString("service")
    val sourceRepositories = config.getConfigList("repositories").asScala.map(RepositoryConfig(_))
    SourceRepository(serviceName, sourceRepositories)
  }
}

case class RepositoryConfig(uri: URI, cloneSubmodules: Boolean, directory: Option[String])

object RepositoryConfig {
  def apply(config: Config): RepositoryConfig = {
    val uri = new URI(config.getString("url"))
    val cloneSubmodules = if (config.hasPath("cloneSubmodules")) config.getBoolean("cloneSubmodules") else true
    val directory = if (config.hasPath("directory")) Some(config.getString("directory")) else None
    RepositoryConfig(uri, cloneSubmodules, directory)
  }
}

case class SourcesConfig(Services: Map[ServiceName, Seq[RepositoryConfig]])

object SourcesConfig {
  def apply(directory: File)(implicit log: Logger): Option[SourcesConfig] = {
    val configFile = new File(directory, "sources.json")
    if (configFile.exists()) {
      val services = IOUtils.parseConfigFile(configFile).getOrElse(return None)
        .getConfigList("sources").asScala.map(SourceRepository(_))
        .foldLeft(Map.empty[ServiceName, Seq[RepositoryConfig]])((map, entry) => map + (entry.serviceName -> entry.sourceRepositories))
      Some(SourcesConfig(services))
    } else {
      log.error(s"Config file ${configFile} does not exist")
      None
    }
  }
}