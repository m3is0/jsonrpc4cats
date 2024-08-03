ThisBuild / tlBaseVersion := "0.3"

ThisBuild / organization := "io.github.m3is0"
ThisBuild / organizationName := "m3is0"
ThisBuild / startYear := Some(2024)
ThisBuild / licenses := Seq(License.Apache2)
ThisBuild / developers := List(tlGitHubDev("m3is0", "Andrii Y."))
ThisBuild / tlCiScalafmtCheck := true
ThisBuild / tlCiHeaderCheck := true
ThisBuild / tlCiDependencyGraphJob := false
ThisBuild / githubWorkflowJavaVersions := Seq(JavaSpec.temurin("21"))

ThisBuild / scalaVersion := "3.3.3"

val MunitV = "1.0.2"
val CatsV = "2.12.0"
val CirceV = "0.14.10"

lazy val jsonrpc4cats = tlCrossRootProject
  .aggregate(
    json,
    circe,
    core,
    server,
    client,
    example
  )

lazy val json = crossProject(JSPlatform, JVMPlatform, NativePlatform)
  .crossType(CrossType.Pure)
  .in(file("modules/json"))
  .settings(
    name := "jsonrpc4cats-json",
    libraryDependencies ++= Seq(
      "org.scalameta" %%% "munit" % MunitV % Test
    )
  )

lazy val circe = crossProject(JSPlatform, JVMPlatform, NativePlatform)
  .crossType(CrossType.Pure)
  .in(file("modules/circe"))
  .settings(
    name := "jsonrpc4cats-circe",
    libraryDependencies ++= Seq(
      "io.circe" %%% "circe-core" % CirceV,
      "io.circe" %%% "circe-parser" % CirceV,
      "org.scalameta" %%% "munit" % MunitV % Test
    )
  )
  .dependsOn(
    json % "test->test;compile->compile"
  )

lazy val core = crossProject(JSPlatform, JVMPlatform, NativePlatform)
  .crossType(CrossType.Pure)
  .in(file("modules/core"))
  .settings(
    name := "jsonrpc4cats-core",
    libraryDependencies ++= Seq(
      "org.typelevel" %%% "cats-core" % CatsV,
      "org.scalameta" %%% "munit" % MunitV % Test
    )
  )
  .dependsOn(
    json,
    circe % Test
  )

lazy val server = crossProject(JSPlatform, JVMPlatform, NativePlatform)
  .crossType(CrossType.Pure)
  .in(file("modules/server"))
  .settings(
    name := "jsonrpc4cats-server",
    libraryDependencies ++= Seq(
      "org.typelevel" %%% "cats-core" % CatsV,
      "org.scalameta" %%% "munit" % MunitV % Test
    )
  )
  .dependsOn(
    core,
    circe % Test
  )

lazy val client = crossProject(JSPlatform, JVMPlatform, NativePlatform)
  .crossType(CrossType.Pure)
  .in(file("modules/client"))
  .settings(
    name := "jsonrpc4cats-client",
    libraryDependencies ++= Seq(
      "org.typelevel" %%% "cats-core" % CatsV,
      "org.scalameta" %%% "munit" % MunitV % Test
    )
  )
  .dependsOn(
    core,
    circe % Test
  )

lazy val example = crossProject(JSPlatform, JVMPlatform, NativePlatform)
  .crossType(CrossType.Pure)
  .in(file("modules/example"))
  .enablePlugins(NoPublishPlugin)
  .settings(
    name := "jsonrpc4cats-example",
    libraryDependencies ++= Seq(
      "org.typelevel" %%% "cats-core" % CatsV,
      "org.scalameta" %%% "munit" % MunitV % Test
    )
  )
  .dependsOn(
    server,
    client,
    circe
  )
