import ReleaseTransformations._

lazy val chainSettings = Seq(
  organization := "org.spire-math",
  licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
  homepage := Some(url("http://github.com/non/chain")),
  scalaVersion := "2.12.2",
  crossScalaVersions := Seq("2.10.6", "2.11.12", "2.12.4"),
  scalacOptions ++= Seq(
    "-feature",
    "-deprecation",
    "-unchecked"),
  libraryDependencies += "org.scalacheck" %%% "scalacheck" % "1.13.5" % "test",
  //scalaJSStage in Global := FastOptStage, //FIXME how to update this for sbt 1.0?
  releaseCrossBuild := true,
  releasePublishArtifactsAction := PgpKeys.publishSigned.value,
  publishMavenStyle := true,
  publishArtifact in Test := false,
  pomIncludeRepository := Function.const(false),
  publishTo := Some(if (isSnapshot.value) Opts.resolver.sonatypeSnapshots else Opts.resolver.sonatypeStaging),
  pomExtra := (
    <scm>
      <url>git@github.com:non/chain.git</url>
      <connection>scm:git:git@github.com:non/chain.git</connection>
    </scm>
    <developers>
      <developer>
        <id>non</id>
        <name>Erik Osheim</name>
        <url>http://github.com/non/</url>
      </developer>
    </developers>
  ),

  releaseProcess := Seq[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    runClean,
    runTest,
    setReleaseVersion,
    commitReleaseVersion,
    tagRelease,
    publishArtifacts,
    setNextVersion,
    commitNextVersion,
    releaseStepCommand("sonatypeReleaseAll"),
    pushChanges))

lazy val noPublish = Seq(
  publish := {},
  publishLocal := {},
  publishArtifact := false)

lazy val root = project
  .in(file("."))
  .aggregate(chainJS, chainJVM)
  .settings(name := "chain-root")
  .settings(chainSettings: _*)
  .settings(noPublish: _*)

lazy val chain = crossProject
  .crossType(CrossType.Pure)
  .in(file("."))
  .settings(name := "chain")
  .settings(chainSettings: _*)

lazy val chainJVM = chain.jvm

lazy val chainJS = chain.js
