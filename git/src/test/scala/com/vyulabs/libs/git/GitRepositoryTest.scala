package com.vyulabs.libs.git

import com.vyulabs.update.common.utils.IoUtils
import org.scalatest.{FlatSpec, Matchers}
import org.slf4j.LoggerFactory

import java.io.{File, IOException}
import java.nio.file.Files

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 18.05.22.
  * Copyright FanDate, Inc.
  */
class GitRepositoryTest extends FlatSpec with Matchers {
  behavior of "GitRepository"

  implicit val log = LoggerFactory.getLogger(this.getClass)

  it should "clone repository" in {
    val dir = Files.createTempDirectory("dir")
    val rep = GitRepository.cloneRepository("ssh://git@bitbucket.org/vyulabs/webrtc.git", "master",
      dir.toFile, false).getOrElse(throw new IOException("Can't create Git repository"))
  }

  it should "work with submodules" in {
    println("*** Create submodules repositories")

    val subDir1 = Files.createTempDirectory("submodule1")
    println(s"***** Submodule 1 directory ${subDir1.toString}")
    val subGit1 = GitRepository.initRepository(subDir1.toFile).getOrElse(throw new IOException("Can't create Git repository"))
    val file1 = new File(subDir1.toFile, "file1")
    assert(IoUtils.writeBytesToFile(file1, "qwertyasdfgh12345".getBytes))
    subGit1.addFile(file1)
    subGit1.commit("First commit")

    val subDir2 = Files.createTempDirectory("submodule2")
    println(s"***** Submodule 2 directory ${subDir2.toString}")
    val subGit2 = GitRepository.initRepository(subDir2.toFile).getOrElse(throw new IOException("Can't create Git repository"))
    val file2 = new File(subDir2.toFile, "file2")
    assert(IoUtils.writeBytesToFile(file2, "asdfghjk09875".getBytes))
    subGit2.addFile(file2)
    subGit2.commit("First commit")

    println("*** Create parent repository with submodules")

    val parentDir = Files.createTempDirectory("parent")
    println(s"***** Parent repository directory ${parentDir.toString}")
    val parent = GitRepository.initRepository(parentDir.toFile).getOrElse(throw new IOException("Can't create Git repository"))
    val file = new File(parentDir.toFile, "file")
    assert(IoUtils.writeBytesToFile(file, "zxcvbnm23874623".getBytes))
    parent.addFile(file)
    parent.addSubmodule(s"${subDir1.toUri}", "module1")
    parent.addSubmodule(s"${subDir2.toUri}", "module2")
    parent.commit("First commit")

    println("*** Clone parent repository")

    val parentDir1 = Files.createTempDirectory("cloned-parent")
    println(s"***** Cloned parent repository directory ${parentDir1.toString}")
    val parent1 = GitRepository.cloneRepository(parentDir.toUri.toString, "master", parentDir1.toFile,
      true).getOrElse(throw new IOException("Can't clone Git repository"))

    assertResult(2)(parent1.getSubmoduleStatus().size)

    println("Update submodule")

    assert(IoUtils.writeBytesToFile(file2, "gfdwqewrf432ds".getBytes))
    subGit2.addFile(file2)
    subGit2.commit("Second commit")

    println("Update repository")

    assert(IoUtils.writeBytesToFile(file, "vncxhh45".getBytes))
    parent.addFile(file)
    parent.commit("Second commit")

    println("*** Pull parent repository")

    val parent2 = GitRepository.openAndPullRepository(parentDir.toUri.toString, "master", parentDir1.toFile,
      true).getOrElse(throw new IOException("Can't pull Git repository"))

    assertResult(2)(parent2.getSubmoduleStatus().size)

    println("*** Add submodule")

    val subDir3 = Files.createTempDirectory("submodule3")
    println(s"***** Submodule 3 directory ${subDir3.toString}")
    val subGit3 = GitRepository.initRepository(subDir3.toFile).getOrElse(throw new IOException("Can't create Git repository"))
    val file3 = new File(subDir3.toFile, "file3")
    assert(IoUtils.writeBytesToFile(file3, "cvbdrjgm233dsfds".getBytes))
    subGit3.addFile(file3)
    subGit3.commit("First commit")

    parent.addSubmodule(s"${subDir3.toUri}", "module3")
    parent.commit("Third commit")

    val parent3 = GitRepository.openAndPullRepository(parentDir.toUri.toString, "master", parentDir1.toFile,
      true).getOrElse(throw new IOException("Can't pull Git repository"))

    assertResult(3)(parent3.getSubmoduleStatus().size)
    val content3 = IoUtils.readFileToBytes(new File(parentDir1.toFile, "module3/file3"))
      .map(new String(_, "utf8")).getOrElse("Can't read file")
    assertResult("cvbdrjgm233dsfds")(content3)

//    TODO check modify module. Can't update module in parent now.
//    println("*** Modify submodule")
//
//    assert(IoUtils.writeBytesToFile(file3, "12edwe3dfsf".getBytes))
//    subGit3.addFile(file3)
//    subGit3.commit("file3 is modified")
//    new GitRepository(Git.open(new File(parent.getDirectory(), "module3"))).pull()
//    parent.commit("Commit with modified module")
//    parent.push()
//
//    val parent4 = GitRepository.openAndPullRepository(parentDir.toUri.toString, "master", parentDir1.toFile,
//      true).getOrElse(throw new IOException("Can't pull Git repository"))
//
//    val content1 = IoUtils.readFileToBytes(new File(parentDir1.toFile, "module3/file3"))
//      .map(new String(_, "utf8")).getOrElse("Can't read file")
//    assertResult("12edwe3dfsf")(content1)
  }
}
