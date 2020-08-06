package com.vyulabs.update.distribution

import java.io.File
import java.net.URL

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.vyulabs.update.common.com.vyulabs.common.utils.Arguments
import com.vyulabs.update.distribution.client.ClientDistributionDirectory
import com.vyulabs.update.distribution.developer.DeveloperDistributionDirectory
import com.vyulabs.update.lock.SmartFilesLocker
import com.vyulabs.update.users.{PasswordHash, UserCredentials, UserRole, UsersCredentials}
import com.vyulabs.update.utils.{IOUtils, Utils}
import distribution.client.ClientDistribution
import distribution.client.config.ClientDistributionConfig
import distribution.client.uploaders.{ClientFaultUploader, ClientLogUploader, ClientStateUploader}
import distribution.developer.DeveloperDistribution
import distribution.developer.config.DeveloperDistributionConfig
import distribution.developer.uploaders.{DeveloperFaultUploader, DeveloperStateUploader}
import org.slf4j.LoggerFactory

import scala.io.StdIn

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 19.04.19.
  * Copyright FanDate, Inc.
  */
object DistributionMain extends App {
  implicit val system = ActorSystem("Distribution")
  implicit val materializer = ActorMaterializer()

  implicit val log = LoggerFactory.getLogger(this.getClass)

  private val directory = new File("directory")

  if (args.size < 1) {
    Utils.error(usage())
  }

  def usage() =
    "Arguments: developer <port=value>\n" +
    "           client <clientName=value> <developerDirectoryUrl=value> <port=value>\n" +
    "           addUser <userName=value> <role=value>\n" +
    "           removeUser <userName=value>\n" +
    "           changePassword <userName=value>"

  try {

    val command = args(0)
    val arguments = Arguments.parse(args.drop(1))

    implicit val filesLocker = new SmartFilesLocker()

    val usersCredentials = UsersCredentials()

    command match {
      case "developer" =>
        val config = DeveloperDistributionConfig().getOrElse {
          Utils.error("No config")
        }

        val dir = new DeveloperDistributionDirectory(directory)

        val stateUploader = new DeveloperStateUploader(dir)
        val faultUploader = new DeveloperFaultUploader(dir)

        val selfUpdater = new SelfUpdater(dir)
        val distribution = new DeveloperDistribution(dir, config.port, usersCredentials, stateUploader, faultUploader)

        selfUpdater.start()

        Runtime.getRuntime.addShutdownHook(new Thread() {
          override def run(): Unit = {
            selfUpdater.close()
          }
        })

        distribution.run()

      case "client" =>
        val config = ClientDistributionConfig().getOrElse {
          Utils.error("No config")
        }

        val dir = new ClientDistributionDirectory(directory)

        val stateUploader = new ClientStateUploader(dir, config.developerDistributionUrl)
        val faultUploader = new ClientFaultUploader(dir, config.developerDistributionUrl)
        val logUploader = new ClientLogUploader(dir)

        val selfUpdater = new SelfUpdater(dir)

        val distribution = new ClientDistribution(dir, config.port, usersCredentials, stateUploader, logUploader, faultUploader)

        stateUploader.start()
        logUploader.start()
        faultUploader.start()
        selfUpdater.start()

        Runtime.getRuntime.addShutdownHook(new Thread() {
          override def run(): Unit = {
            stateUploader.close()
            logUploader.close()
            faultUploader.close()
            selfUpdater.close()
          }
        })

        distribution.run()

      case "addUser" =>
        val userName = arguments.getValue("userName")
        val role = UserRole.withName(arguments.getValue("role"))
        val password = StdIn.readLine("Enter password: ")
        if (usersCredentials.getCredentials(userName).isDefined) {
          Utils.error(s"User ${userName} credentials already exists")
        }
        usersCredentials.addUser(userName, UserCredentials(role, PasswordHash(password)))
        if (!usersCredentials.save()) {
          Utils.error("Can't save credentials file")
        }
        sys.exit()

      case "removeUser" =>
        val userName = arguments.getValue("userName")
        usersCredentials.removeUser(userName)
        if (!usersCredentials.save()) {
          Utils.error("Can't save credentials file")
        }
        sys.exit()

      case "changePassword" =>
        val userName = arguments.getValue("userName")
        val password = StdIn.readLine("Enter password: ")
        usersCredentials.getCredentials(userName) match {
          case Some(credentials) =>
            credentials.passwordHash = PasswordHash(password)
            if (!usersCredentials.save()) {
              Utils.error("Can't save credentials file")
            }
          case None =>
            Utils.error(s"No user ${userName} credentials")
        }
        sys.exit()

      case _ =>
        Utils.error(s"Invalid command ${command}\n${usage()}")
    }
  } catch {
    case ex: Throwable =>
      log.error("Exception", ex)
      Utils.error(ex.getMessage)
      sys.exit(1)
  }
}