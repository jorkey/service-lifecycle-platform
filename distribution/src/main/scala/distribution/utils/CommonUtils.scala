package distribution.utils

import java.io.{File}

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.Directives.{getFromBrowseableDirectory, getFromFile, _}
import akka.http.scaladsl.server.{Route}
import com.vyulabs.update.distribution.DistributionDirectory
import org.slf4j.LoggerFactory

trait CommonUtils extends SprayJsonSupport {
  private implicit val log = LoggerFactory.getLogger(this.getClass)

  implicit val dir: DistributionDirectory

  def browse(path: Option[String]): Route = {
    val file = path match {
      case Some(path) =>
        new File(dir.directory, path)
      case None =>
        dir.directory
    }
    if (file.isDirectory) {
      getFromBrowseableDirectory(file.getPath)
    } else {
      getFromFile(file.getPath)
    }
  }
}
