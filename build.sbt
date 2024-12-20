ThisBuild / tlBaseVersion := "0.6"

ThisBuild / organization := "io.github.m3is0"
ThisBuild / organizationName := "m3is0"
ThisBuild / startYear := Some(2024)
ThisBuild / licenses := Seq(License.Apache2)
ThisBuild / developers := List(tlGitHubDev("m3is0", "Andrii Y."))
ThisBuild / tlCiScalafmtCheck := true
ThisBuild / tlCiHeaderCheck := true
ThisBuild / tlCiDependencyGraphJob := false
ThisBuild / githubWorkflowJavaVersions := Seq(JavaSpec.temurin("21"))
ThisBuild / tlSitePublishBranch := Some("main")

ThisBuild / scalaVersion := "3.3.4"

val MunitV = "1.0.3"
val MunitCatsEffectV = "2.0.0"

val CatsV = "2.12.0"
val CirceV = "0.14.10"
val Http4sV = "0.23.30"

lazy val jsonrpc4cats = tlCrossRootProject
  .aggregate(
    json,
    circe,
    core,
    server,
    client,
    http4s,
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

lazy val http4s = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("modules/http4s"))
  .settings(
    name := "jsonrpc4cats-http4s",
    libraryDependencies ++= Seq(
      "org.http4s" %%% "http4s-dsl" % Http4sV,
      "org.http4s" %%% "http4s-server" % Http4sV,
      "org.typelevel" %%% "munit-cats-effect" % MunitCatsEffectV % Test
    )
  )
  .dependsOn(
    server,
    circe % Test
  )

lazy val example = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("modules/example"))
  .enablePlugins(NoPublishPlugin)
  .settings(
    name := "jsonrpc4cats-example",
    libraryDependencies ++= Seq(
      "org.typelevel" %%% "cats-core" % CatsV,
      "io.circe" %%% "circe-generic" % CirceV,
      "org.typelevel" %%% "munit-cats-effect" % MunitCatsEffectV % Test
    )
  )
  .dependsOn(
    server,
    client,
    http4s,
    circe
  )

lazy val docs = project
  .in(file("site"))
  .enablePlugins(TypelevelSitePlugin)
  .settings(
    tlSiteIsTypelevelProject := None,
    laikaTheme := {
      // scalafmt: { newlines.source = keep }
      import laika.ast.*
      import laika.helium.config.*
      tlSiteHelium.value
        .site.topNavigationBar(
          homeLink = TextLink.internal(Path.Root / "README.md", "jsonrpc4cats")
        )
        .site.footer(
          Text("jsonrpc4cats is designed and developed by "),
          SpanLink.external("https://github.com/m3is0/jsonrpc4cats")(Literal("m3is0"))
        )
        .build
    }
  )
