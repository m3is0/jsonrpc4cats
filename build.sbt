ThisBuild / tlBaseVersion := "0.1"

ThisBuild / organization := "io.github.m3is0"
ThisBuild / organizationName := "m3is0"
ThisBuild / startYear := Some(2024)
ThisBuild / licenses := Seq(License.Apache2)
ThisBuild / developers ++= List(tlGitHubDev("m3is0", "Andrii Y."))
ThisBuild / tlCiScalafmtCheck := true
ThisBuild / tlCiHeaderCheck := true
ThisBuild / tlCiDependencyGraphJob := false
ThisBuild / githubWorkflowJavaVersions := List(JavaSpec.temurin("21"))

ThisBuild / scalaVersion := "3.3.3"

val commonSettings = Seq(
  sonatypeCredentialHost := xerial.sbt.Sonatype.sonatypeCentralHost
)

val MunitV = "1.0.0"
val CatsV = "2.12.0"
val CirceV = "0.14.9"

lazy val root = tlCrossRootProject
  .aggregate(
    core,
    server,
    circe,
    example
  )

lazy val core = crossProject(JSPlatform, JVMPlatform, NativePlatform)
  .crossType(CrossType.Pure)
  .in(file("modules/core"))
  .settings(commonSettings)
  .settings(
    name := "jsonrpc4cats-core",
    libraryDependencies ++= Seq(
      "org.typelevel" %%% "cats-core" % CatsV,
      "org.scalameta" %%% "munit" % MunitV % Test
    )
  )

lazy val server = crossProject(JSPlatform, JVMPlatform, NativePlatform)
  .crossType(CrossType.Pure)
  .in(file("modules/server"))
  .settings(commonSettings)
  .settings(
    name := "jsonrpc4cats-server",
    libraryDependencies ++= Seq(
      "org.typelevel" %%% "cats-core" % CatsV,
      "org.scalameta" %%% "munit" % MunitV % Test
    )
  )
  .dependsOn(
    core,
    circe % "test"
  )

lazy val circe = crossProject(JSPlatform, JVMPlatform, NativePlatform)
  .crossType(CrossType.Pure)
  .in(file("modules/circe"))
  .settings(commonSettings)
  .settings(
    name := "jsonrpc4cats-circe",
    libraryDependencies ++= Seq(
      "io.circe" %%% "circe-core" % CirceV,
      "io.circe" %%% "circe-parser" % CirceV,
      "org.scalameta" %%% "munit" % MunitV % Test
    )
  )
  .dependsOn(
    core % "test->test;compile->compile"
  )

lazy val example = crossProject(JSPlatform, JVMPlatform, NativePlatform)
  .crossType(CrossType.Pure)
  .in(file("modules/example"))
  .enablePlugins(NoPublishPlugin)
  .settings(commonSettings)
  .settings(
    name := "jsonrpc4cats-example",
    libraryDependencies ++= Seq(
      "org.typelevel" %%% "cats-core" % CatsV,
      "org.scalameta" %%% "munit" % MunitV % Test
    )
  )
  .dependsOn(
    server,
    circe
  )
