name := """reactive_futures"""
organization := "com.example"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.11"

libraryDependencies += filters

libraryDependencies ++= Seq(
  ws,
  specs2,
  "org.reactivemongo" %% "play2-reactivemongo" % "0.11.7.play24",
  "javax.inject" % "javax.inject" % "1",
  "joda-time" % "joda-time" % "2.9.9"
)



// Adds additional packages into Twirl
//TwirlKeys.templateImports += "com.example.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "com.example.binders._"
