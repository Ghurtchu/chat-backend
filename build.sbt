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
      "org.tpolecat" %% "doobie-core"      % "1.0.0-RC4",
      "org.tpolecat" %% "doobie-postgres"  % "1.0.0-RC4", // Postgres driver 42.6.0 + type mappings.
      "com.github.pureconfig" %% "pureconfig" % "0.17.4"
    ),
    scalacOptions ++= Seq(
      "-unchecked",
      "-deprecation",
      "-feature",
      "-language:postfixOps",
      "-language:higherKinds"
    )
  )
