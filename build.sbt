import sbt.project

import scala.sys.process.Process

name := "update"

scalaVersion in ThisBuild := "2.12.12"

parallelExecution in Test := false
concurrentRestrictions in Global += Tags.limit(Tags.Test, 1)

trapExit in builder := false

lazy val update = project
  .in(file("."))
  .disablePlugins(sbtassembly.AssemblyPlugin)
  .aggregate(
    common,
    builder,
    updater,
    distribution,
    tests
  )

lazy val common = project
  .in(file("common"))
  .disablePlugins(sbtassembly.AssemblyPlugin)
  .settings(
    libraryDependencies ++= (baseDependencies ++ sprayDependencies)
  )

lazy val builder = project
  .in(file("builder"))
  .settings(
    libraryDependencies ++= baseDependencies,
    assemblySettings
  )
  .dependsOn(
    gitLib,
    distribution % "test->test"
  )

lazy val updater = project
  .in(file("updater"))
  .settings(
    libraryDependencies ++= baseDependencies,
    assemblySettings
  )
  .dependsOn(
    common % "compile->compile;test->test"
  )

lazy val distribution = project
  .in(file("distribution"))
  .settings(
    libraryDependencies ++= (baseDependencies ++ akkaDependencies ++ mongoDependencies ++ sangriaDependencies),
    assemblySettings
  )
  .dependsOn(
    common % "compile->compile;test->test",
    dashboard
  )

lazy val dashboard = project
  .in(file("dashboard"))
  .settings(
    resourceGenerators in Compile += buildDashboard.init
  )

lazy val tests = project
  .in(file("tests"))
  .dependsOn(
    gitLib,
    common % "compile->compile;test->test",
    distribution % "compile->compile;test->test",
    builder % "compile->compile;test->test",
    updater % "compile->compile;test->test"
  )

lazy val gitLib = project
  .in(file("git"))
  .settings(
    libraryDependencies ++= baseDependencies ++ Seq(
      dependencies.jGit
    )
  )
  .dependsOn(
    common % "compile->compile;test->test"
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
  dependencies.sprayJson,
  dependencies.sprayJwt
)

lazy val mongoDependencies = Seq(
  dependencies.mongoDbDriver,
  dependencies.mongoDbScalaBson,
  //dependencies.mongoDbAkka
)

lazy val sangriaDependencies = Seq(
  dependencies.sangria,
  dependencies.sangriaSprayJson,
  dependencies.sangriaAkkaStreams,
  dependencies.sangriaSlowlog
)

lazy val dependencies =
  new {
    val akkaVersion = "2.6.9"
    val akkaHttpVersion = "10.2.1"

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

    // Misc
    val jGit = "org.eclipse.jgit" % "org.eclipse.jgit" % "6.0.0.202111291000-r"

    // MongoDB
    val mongoDbDriver = "org.mongodb" % "mongodb-driver-reactivestreams" % "1.13.1"
    val mongoDbScalaBson = "org.mongodb.scala" %% "mongo-scala-bson" % "4.1.1"
    val mongoDbAkka = "com.lightbend.akka" %% "akka-stream-alpakka-mongodb" % "2.0.2"

    // Spray
    val sprayJson = "io.spray" %% "spray-json" % "1.3.5"
    val sprayJwt = "com.github.janjaali" %% "spray-jwt" % "1.0.0"

    // Sangria
    val sangria = "org.sangria-graphql" %% "sangria" % "2.0.0"
    val sangriaSprayJson = "org.sangria-graphql" %% "sangria-spray-json" % "1.0.1"
    val sangriaAkkaStreams = "org.sangria-graphql" %% "sangria-akka-streams" % "1.0.2"
    val sangriaSlowlog = "org.sangria-graphql" %% "sangria-slowlog" % "2.0.1"
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
//  task.log.info(s"Delete npm modules ...")
//  IO.delete(baseDirectory.value / "node_modules")
  task.log.info("Install npm modules ...")
  val process = Process(shell :+ "npm install", baseDirectory.value)
  if ((process ! task.log) != 0) {
    throw new IllegalStateException("Can't install npm modules")
  }
}

lazy val buildReactScripts = taskKey[Unit]("Compile React scripts") := {
  installNpmModules.init.value // Comment this to skip compile React scripts
  val task = streams.value
  task.log.info(s"Delete React build ...")
  IO.delete(baseDirectory.value / "build")
  task.log.info("Compile React scripts ...")
  val process = Process(shell :+ s"npm run-script build", baseDirectory.value)
  if ((process ! task.log) != 0) {
    throw new IllegalStateException("Can't compile React scripts")
  }
}

lazy val buildDashboard = taskKey[Seq[File]]("Generate dashboard resources") := {
  buildReactScripts.init.value // Comment this to skip compile React scripts
  val webapp = baseDirectory.value / "build"
  val managed = resourceManaged.value
  for {
    (from, to) <- webapp ** "*" pair Path.rebase(webapp, managed / "main" / "ui")
  } yield {
    Sync.copy(from, to)
    to
  }
}
