import Dependencies._

ThisBuild / scalaVersion     := "2.13.8"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "io.github.go4ble"
ThisBuild / organizationName := "go4ble"

lazy val root = (project in file("."))
  .settings(
    name := "hermes-intent-handler",
    libraryDependencies += scalaTest % Test,
    libraryDependencies += "com.typesafe.akka" %% "akka-actor-typed" % "2.6.20",
    libraryDependencies += "com.typesafe.akka" %% "akka-stream" % "2.6.20",
    libraryDependencies += "com.typesafe.akka" %% "akka-http" % "10.2.10",
    libraryDependencies += "com.typesafe.play" %% "play-json" % "2.9.3",
    libraryDependencies += "org.eclipse.paho" % "org.eclipse.paho.client.mqttv3" % "1.2.5",
    libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.4.3"
  )
