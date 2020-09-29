import sbt.project

import scala.sys.process.Process

name := "update"

scalaVersion in ThisBuild := "2.12.8"

lazy val update = project
  .in(file("."))
  .disablePlugins(sbtassembly.AssemblyPlugin)
  .aggregate(
    updateCommon,
    builder,
    installer,
    updater,
    distribution
  )

lazy val updateCommon = project
  .in(file("common"))
  .disablePlugins(sbtassembly.AssemblyPlugin)
  .settings(
    libraryDependencies ++= (baseDependencies ++ sprayDependencies)
  )

lazy val updateAdmin = project
  .in(file("admin"))
  .disablePlugins(sbtassembly.AssemblyPlugin)
  .settings(
    libraryDependencies ++= baseDependencies
  )
  .dependsOn(
    updateCommon
  )
  .dependsOn(
    gitLib
  )

lazy val builder = project
  .in(file("builder"))
  .settings(
    libraryDependencies ++= baseDependencies,
    assemblySettings
  )
  .dependsOn(
    updateAdmin
  )

lazy val installer = project
  .in(file("installer"))
  .settings(
    libraryDependencies ++= baseDependencies,
    assemblySettings
  )
  .dependsOn(
    updateAdmin
  )

lazy val updater = project
  .in(file("updater"))
  .settings(
    libraryDependencies ++= baseDependencies,
    assemblySettings
  )
  .dependsOn(
    updateCommon
  )

lazy val distribution = project
  .in(file("distribution"))
  .settings(
    libraryDependencies ++= (baseDependencies ++ akkaDependencies),
    assemblySettings
  )
  .dependsOn(
    updateCommon,
    dashboard
  )

lazy val dashboard = project
  .in(file("dashboard"))
  .settings(
    resourceGenerators in Compile += buildUi.init
  )

lazy val gitLib = project
  .in(file("git"))
  .settings(
    libraryDependencies ++= baseDependencies ++ Seq(
      dependencies.jGit
    )
  )

lazy val baseDependencies = Seq(
  dependencies.config,
  dependencies.logback,
  dependencies.junit,
  dependencies.junitInterface,
  dependencies.scalaMock,
  dependencies.scalaTest
)

lazy val akkaDependencies = Seq(
  dependencies.akkaActor,
  dependencies.akkaStream,
  dependencies.akkaTestKit,
  dependencies.akkaSlf4j,
  dependencies.akkaStreamTestKit,
  dependencies.akkaHttp,
  dependencies.akkaHttpSpray,
  dependencies.akkaHttpTestKit
)

lazy val sprayDependencies = Seq(
  dependencies.sprayJson
)

lazy val dependencies =
  new {
    val akkaVersion = "2.6.7"
    val akkaHttpVersion = "10.1.12"

    // Logging
    val logback = "ch.qos.logback" % "logback-classic" % "1.2.3"

    // Config
    val config = "com.typesafe" % "config" % "1.3.2"

    // Tests
    val junit = "junit" % "junit" % "4.12" % "test"
    val junitInterface = "com.novocode" % "junit-interface" % "0.11" % "test"
    val scalaMock = "org.scalamock" %% "scalamock" % "4.0.0" % "test"
    val scalaTest = "org.scalatest" %% "scalatest" % "3.0.4" % "test"

    // Akka
    val akkaActor = "com.typesafe.akka" %% "akka-actor" % akkaVersion
    val akkaStream = "com.typesafe.akka" %% "akka-stream" % akkaVersion
    val akkaTestKit = "com.typesafe.akka" %% "akka-testkit" % akkaVersion
    val akkaSlf4j = "com.typesafe.akka" %% "akka-slf4j" % akkaVersion
    val akkaStreamTestKit = "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion
    val akkaHttp = "com.typesafe.akka" %% "akka-http" % akkaHttpVersion
    val akkaHttpSpray = "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion
    val akkaHttpTestKit = "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion

    // Spray Json
    val sprayJson = "io.spray" %% "spray-json" % "1.3.5"

    // Misc
    val jGit = "org.eclipse.jgit" % "org.eclipse.jgit" % "5.6.1.202002131546-r"
  }

lazy val assemblySettings = Seq(
  assemblyMergeStrategy in assembly := {
    case PathList("reference.conf") => MergeStrategy.concat // Akka configuration file
    case PathList("META-INF", xs@_*) => { // Extract native libraries
      if (!xs.isEmpty && xs(0) == "native") {
        MergeStrategy.concat
      } else {
        MergeStrategy.discard
      }
    }
    case _ => MergeStrategy.first
  }
)

val shell = if (sys.props("os.name").contains("Windows")) Seq("cmd", "/c") else Seq ("bash", "-c")

lazy val installNpmModules = taskKey[Unit]("Install npm modules") := {
  val task = streams.value
  //task.log.info(s"Delete npm modules ...")
  //IO.delete(baseDirectory.value / "node_modules")
  task.log.info("Install npm modules ...")
  val process = Process(shell :+ "npm install", baseDirectory.value)
  if ((process ! task.log) != 0) {
    throw new IllegalStateException("Can't install npm modules")
  }
}

lazy val buildReactScripts = taskKey[Unit]("Compile React scripts") := {
  installNpmModules.init.value
  val task = streams.value
  task.log.info(s"Delete React build ...")
  IO.delete(baseDirectory.value / "build")
  task.log.info("Compile React scripts ...")
  val process = Process(shell :+ s"npm run-script build", baseDirectory.value)
  if ((process ! task.log) != 0) {
    throw new IllegalStateException("Can't compile React scripts")
  }
}

lazy val buildUi = taskKey[Seq[File]]("Generate dashboard resources") := {
  buildReactScripts.init.value
  val webapp = baseDirectory.value / "build"
  val managed = resourceManaged.value
  for {
    (from, to) <- webapp ** "*" pair Path.rebase(webapp, managed / "main" / "ui")
  } yield {
    Sync.copy(from, to)
    to
  }
}

