import Dependencies._

ThisBuild / scalaVersion     := "2.13.8"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "io.github.go4ble"
ThisBuild / organizationName := "go4ble"

lazy val root = (project in file("."))
  .settings(
    name := "hermes-intent-handler",
    libraryDependencies += scalaTest % Test,
    libraryDependencies += "com.typesafe.akka" %% "akka-actor-typed" % "2.7.0",
    libraryDependencies += "com.typesafe.akka" %% "akka-stream" % "2.7.0",
    libraryDependencies += "com.typesafe.akka" %% "akka-http" % "10.4.0",
    libraryDependencies += "com.typesafe.play" %% "play-json" % "2.9.3",
    libraryDependencies += "com.typesafe" % "config" % "1.4.2",
    libraryDependencies += "org.eclipse.paho" % "org.eclipse.paho.client.mqttv3" % "1.2.5",
    libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.4.4"
  )

(Compile / compile) := ((Compile / compile) dependsOn scalafmtCheckAll).value

enablePlugins(JavaAppPackaging)
dockerBaseImage := "eclipse-temurin:17-alpine"
dockerRepository := Some("ghcr.io/go4ble")
dockerUpdateLatest := true
