ThisBuild / organization := "io.github.m3is0"
ThisBuild / organizationName := "m3is0"
ThisBuild / startYear := Some(2024)
ThisBuild / licenses := Seq(License.Apache2)
ThisBuild / homepage := Some(url("https://github.com/m3is0/jsonrpc4cats"))
ThisBuild / developers := List(
  Developer("m3is0", "Andrii Y.", "@m3is0", url("https://github.com/m3is0"))
)

ThisBuild / githubWorkflowJavaVersions := List(JavaSpec.temurin("21"))

ThisBuild / githubWorkflowTargetTags ++= Seq("v*")
ThisBuild / githubWorkflowPublishTargetBranches := Seq(RefPredicate.StartsWith(Ref.Tag("v")))

ThisBuild / githubWorkflowBuild := Seq(
  WorkflowStep.Sbt(
    name = Some("Check headers and formatting"),
    commands = List("headerCheckAll", "scalafmtCheckAll", "scalafmtSbtCheck")
  ),
  WorkflowStep.Sbt(
    name = Some("Build and test"),
    commands = List("test")
  )
)

ThisBuild / githubWorkflowPublish := Seq(
  WorkflowStep.Sbt(
    name = Some("Publish project"),
    commands = List("ci-release"),
    env = Map(
      "PGP_PASSPHRASE" -> "${{ secrets.PGP_PASSPHRASE }}",
      "PGP_SECRET" -> "${{ secrets.PGP_SECRET }}",
      "SONATYPE_PASSWORD" -> "${{ secrets.SONATYPE_PASSWORD }}",
      "SONATYPE_USERNAME" -> "${{ secrets.SONATYPE_USERNAME }}"
    )
  )
)

ThisBuild / sonatypeCredentialHost :=
  xerial.sbt.Sonatype.sonatypeCentralHost

ThisBuild / scalaVersion := "3.3.3"

val MunitV = "1.0.0"
val CatsV = "2.12.0"
val CirceV = "0.14.9"

val NoPublish = Seq(
  publish / skip := true,
  publishLocal / skip := true
)

lazy val root = project
  .in(file("."))
  .settings(NoPublish)
  .aggregate(
    core.jvm,
    core.js,
    core.native,
    server.jvm,
    server.js,
    server.native,
    circe.jvm,
    circe.js,
    circe.native,
    example.jvm,
    example.js,
    example.native
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
    circe % "test"
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
    core % "test->test;compile->compile"
  )

lazy val example = crossProject(JSPlatform, JVMPlatform, NativePlatform)
  .crossType(CrossType.Pure)
  .in(file("modules/example"))
  .settings(NoPublish)
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
