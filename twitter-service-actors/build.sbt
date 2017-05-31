name := """twitter-service-actors"""
organization := "app.reactive"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.11"

libraryDependencies += filters
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "2.0.0" % Test
libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.4.0"
libraryDependencies += "com.typesafe.akka" %% "akka-slf4j" % "2.4.0"

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "app.reactive.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "app.reactive.binders._"
