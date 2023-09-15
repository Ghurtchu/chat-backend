ThisBuild / version := "1.0"
ThisBuild / scalaVersion := "2.13.12"

val Http4sVersion = "0.23.23"

lazy val root = (project in file("."))
  .settings(
    name := "ChatServer",
    libraryDependencies ++= Seq(
      "org.http4s" %% "http4s-ember-server" % Http4sVersion,
      "org.http4s" %% "http4s-ember-client" % Http4sVersion,
      "org.http4s" %% "http4s-dsl" % Http4sVersion,
      "org.typelevel" %% "cats-effect" % "3.2.8",
      "org.http4s" %% "http4s-circe" % "0.23.0",
      "io.circe" %% "circe-generic" % "0.14.1"
    ),
    scalacOptions ++= Seq(
      "-unchecked",
      "-deprecation",
      "-feature",
      "-language:postfixOps",
      "-language:higherKinds"
    )
  )
