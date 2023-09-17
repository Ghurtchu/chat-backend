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
      "org.typelevel" %% "cats-effect" % "3.4.8",
      "org.http4s" %% "http4s-circe" % "0.23.18",
      "io.circe" %% "circe-generic" % "0.14.5",
      "dev.profunktor" %% "redis4cats-effects" % "1.4.1",
      "dev.profunktor" %% "redis4cats-streams" % "1.4.1",
      "com.typesafe.akka" %% "akka-actor" % "2.6.16"

    ),
    scalacOptions ++= Seq(
      "-unchecked",
      "-deprecation",
      "-feature",
      "-language:postfixOps",
      "-language:higherKinds"
    )
  )
