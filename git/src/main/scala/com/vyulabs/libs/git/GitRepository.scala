package com.vyulabs.libs.git

import com.vyulabs.update.common.utils.IoUtils
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ListBranchCommand.ListMode
import org.eclipse.jgit.api.errors.RefAlreadyExistsException
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.lib.SubmoduleConfig.FetchRecurseSubmodulesMode
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.transport.RefSpec
import org.slf4j.Logger

import java.io.File
import java.nio.file.Path
import scala.collection.JavaConverters._

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 24.12.18.
  * Copyright FanDate, Inc.
  */
class GitRepository(git: Git)(implicit log: Logger) {
  def getUrl(): String ={
    val url = git.getRepository.getConfig().getString("remote", "origin", "url")
    if (url != null) {
      return url
    }
    "file://" + git.getRepository.getWorkTree.toString
  }

  def getDirectory(): File = {
    git.getRepository.getWorkTree
  }

  def getBranch(): String = {
    git.getRepository.getBranch
  }

  def setBare(bare: Boolean): Boolean = {
    try {
      val config = git.getRepository.getConfig
      config.setString("core", null, "bare", bare.toString)
      config.save()
      true
    } catch {
      case ex: Exception =>
      log.error("Set bare error", ex)
      false
    }
  }

  def checkout(name: Option[String]): Boolean = {
    log.debug(s"Checkout ${name}")
    try {
      val co = git.checkout()
      name.foreach(co.setName(_))
      co.call()
      true
    } catch {
      case ex: Exception =>
        log.error("Checkout error", ex)
        false
    }
  }

  def add(file: File): Boolean = {
    log.debug(s"Add to commit ${file}")
    try {
      git.add().addFilepattern(getSubPath(file).toString).call()
      true
    } catch {
      case ex: Exception =>
        log.error("Add file error", ex)
        false
    }
  }

  def addContents(file: File): Boolean = {
    val contents = file.listFiles()
    if (contents != null) {
      for (file <- contents) {
        if (!add(file)) {
          return false
        }
      }
    }
    true
  }

  def remove(file: File): Boolean = {
    log.debug(s"Remove ${file}")
    try {
      git.rm().addFilepattern(getSubPath(file).toString).call()
      true
    } catch {
      case ex: Exception =>
        log.error("Remove file error", ex)
        false
    }
  }

  def pull(branch: Option[String] = None): Boolean = {
    log.debug(s"Pull ${branch}")
    try {
      val pull = git.pull()
      branch.foreach(pull.setRemoteBranchName(_))
      val result = pull.call()
      if (result.isSuccessful) {
        log.debug("Pull result: " + result.toString)
        true
      } else {
        log.error(s"Pull unsuccessful. Conflicts: ${result.getMergeResult.getCheckoutConflicts.asScala}")
        false
      }
    } catch {
      case ex: Exception =>
        log.error("Pull error", ex)
        false
    }
  }

  def commit(message: String): Boolean = {
    log.debug(s"Commit ${message}")
    try {
      git.commit().setMessage(message).call()
      true
    } catch {
      case ex: Exception =>
        log.error("Commit error", ex)
        false
    }
  }

  final def push(refSpec: Seq[RefSpec] = List.empty, retryWithPull: Boolean = true, force: Boolean = false): Boolean = {
    log.debug(s"Push")
    var command = git.push().setForce(force)
    if (!refSpec.isEmpty) {
      command = command.setRefSpecs(refSpec.asJava)
    }
    val results = try {
      command.call().asScala.toSeq
    } catch {
      case ex: Exception =>
        log.error("Push error", ex)
        return false
    }
    import org.eclipse.jgit.transport.RemoteRefUpdate
    var needRetryWithPull = false
    for (result <- results) {
      for (update <- result.getRemoteUpdates().asScala.toSeq) {
        if (update.getStatus != RemoteRefUpdate.Status.OK) {
          if (update.getStatus == RemoteRefUpdate.Status.REJECTED_NONFASTFORWARD && retryWithPull) {
            needRetryWithPull = true
          } else {
            if (update.getMessage != null) {
              log.error(s"Push error: ${update.getStatus} ${update.getMessage}")
            } else {
              log.error(s"Push error: ${update.getStatus}")
            }
            return false
          }
        }
      }
    }
    if (needRetryWithPull) {
      if (!pull()) {
        return false
      }
      push(refSpec, retryWithPull)
    } else {
      true
    }
  }

  def getCurrentCommit(): Option[RevCommit] = {
    try {
      Some(git.log().call().iterator().next())
    } catch {
      case ex: Exception =>
        log.error("Git log error", ex)
        None
    }
  }

  def setTag(tag: String, message: Option[String], force: Boolean = false): Boolean = {
    log.debug(s"Set tag ${tag}")
    try {
      val command = git.tag().setName(tag).setAnnotated(true).setForceUpdate(force)
      message.foreach(command.setMessage(_))
      command.call()
      true
    } catch {
      case _: RefAlreadyExistsException =>
        log.error(s"Tag ${tag} already exists")
        false
      case ex: Exception =>
        log.error(s"Set tag ${tag} error", ex)
        false
    }
  }

  def deleteTag(tag: String): Boolean = {
    log.debug(s"Delete tag ${tag}")
    try {
      val command = git.tagDelete()
      command.call()
      true
    } catch {
      case ex: Exception =>
        log.error(s"Delete tag ${tag} error", ex)
        false
    }
  }

  def getRefCommits(ref: Ref): Seq[RevCommit] = {
    try {
      val log = git.log
      val peeledRef = git.getRepository.getRefDatabase.peel(ref)
      if (peeledRef.getPeeledObjectId != null) {
        log.add(peeledRef.getPeeledObjectId)
      } else {
        log.add(ref.getObjectId)
      }
      log.call().asScala.toSeq
    } catch {
      case ex: Exception =>
        log.error("Git log error", ex)
        Seq.empty
    }
  }

  def getAllTags(): (Boolean, Seq[Ref]) = {
    log.debug(s"Get tags list")
    try {
      (true, git.tagList().call().asScala)
    } catch {
      case ex: Exception =>
        log.error("Get tags error", ex)
        (false, Seq.empty)
    }
  }

  def getAllBranches(): (Boolean, Seq[Ref]) = {
    log.debug(s"Get branch list")
    import scala.collection.JavaConverters._
    try {
      (true, git.branchList().setListMode(ListMode.ALL).call().asScala)
    } catch {
      case ex: Exception =>
        log.error("Get branches error", ex)
        (false, Seq.empty)
    }
  }

  def createNewBranch(branch: String): Boolean = {
    log.debug(s"Create branch ${branch}")
    try {
      git.branchCreate().setName(branch).call()
      true
    } catch {
      case ex: Exception =>
        log.error(s"Create branch ${branch} error", ex)
        false
    }
  }

  def getLastCommitMessage(): Option[(Long, String)] = {
    try {
      val it = git.log().setMaxCount(1).call().iterator()
      if (it.hasNext) {
        val commit = it.next()
        Some((commit.getCommitTime, commit.getFullMessage))
      } else {
        None
      }
    } catch {
      case ex: Exception =>
        log.error(s"Get last commit message error", ex)
        None
    }
  }

  def close(): Unit = {
    git.close()
  }

  private def getSubPath(file: File): Path = {
    getDirectory().toPath.relativize(file.toPath)
  }
}

object GitRepository {
  def initRepository(directory: File)(implicit log: Logger): Option[GitRepository] = {
    try {
      val git = Git.init().setDirectory(directory).call()
      val config = git.getRepository.getConfig
      config.setString("remote", "origin", "url", "file://" + directory.toString)
      config.save()
      Some(new GitRepository(git))
    } catch {
      case ex:Exception =>
        log.error("Init Git repository error", ex)
        None
    }
  }

  def cloneRepository(uri: String, branch: String, directory: File, cloneSubmodules: Boolean)(implicit log: Logger): Option[GitRepository] = {
    log.info(s"Clone repository ${uri} to ${directory}")
    try {
      if (uri.startsWith("file://")) {
        IoUtils.copyFile(new File(uri.substring(7)), directory)
        Some(new GitRepository(Git.open(directory)))
      } else {
        val git = Git.cloneRepository()
          .setURI(uri)
          .setBranch(branch)
          .setDirectory(directory)
          .setCloneSubmodules(cloneSubmodules)
          .call()
        Some(new GitRepository(git))
      }
    } catch {
      case ex:Exception =>
        log.error("Clone Git repository error", ex)
        None
    }
  }

  def openRepository(directory: File)(implicit log: Logger): Option[GitRepository] = {
    log.info(s"Open Git repository in directory ${directory}")
    try {
      val git = Git.open(directory)
      Some(new GitRepository(git))
    } catch {
      case ex:Exception =>
        log.error("Clone Git repository error", ex)
        None
    }
  }

  def getGitRepository(uri: String, branch: String, cloneSubmodules: Boolean, directory: File)(implicit log: Logger): Option[GitRepository] = {
    if (!directory.exists()) {
      cloneRepository(uri, branch, directory, cloneSubmodules)
    } else {
      openAndPullRepository(uri, branch, directory) match {
        case None =>
          if (!IoUtils.deleteFileRecursively(directory)) {
            return None
          }
          cloneRepository(uri, branch, directory, cloneSubmodules)
        case rep =>
          rep
      }
    }
  }

  def openAndPullRepository(uri: String, branch: String, directory: File)(implicit log: Logger): Option[GitRepository] = {
    log.info(s"Open and pull repository in directory ${directory}")
    var attempt = 1
    while (true) {
      var toClose = Option.empty[Git]
      try {
        val git = Git.open(directory)
        toClose = Some(git)
        val url = git.getRepository.getConfig().getString("remote", "origin", "url")
        if (url != uri) {
          log.info(s"Current URL ${url} != ${uri}")
          return None
        }
        if (git.getRepository.getBranch() != branch) {
          log.info(s"Current branch is ${git.getRepository.getBranch()}. Need branch ${branch}")
          return None
        }
        if (!url.startsWith("file://")) {
          git.pull().setRecurseSubmodules(FetchRecurseSubmodulesMode.YES).call()
        }
        toClose = None
        return Some(new GitRepository(git))
      } catch {
        case ex: Exception =>
          if (attempt == 3) {
            log.error("Open Git repository error", ex)
            return None
          }
          attempt += 1
      } finally {
        toClose.foreach(_.close())
      }
    }
    None
  }
}
