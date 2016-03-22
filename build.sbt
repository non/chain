import ReleaseTransformations._

lazy val chainSettings = Seq(
  organization := "org.spire-math",
  licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
  homepage := Some(url("http://github.com/striation/chain")),
  scalaVersion := "2.11.8",
  crossScalaVersions := Seq("2.10.6", "2.11.8"),
  scalacOptions ++= Seq(
    "-feature",
    "-deprecation",
    "-unchecked"),
  releaseCrossBuild := true,
  releasePublishArtifactsAction := PgpKeys.publishSigned.value,
  publishMavenStyle := true,
  publishArtifact in Test := false,
  pomIncludeRepository := Function.const(false),
  publishTo <<= (version).apply { v =>
    val nexus = "https://oss.sonatype.org/"
    if (v.trim.endsWith("SNAPSHOT"))
      Some("Snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("Releases" at nexus + "service/local/staging/deploy/maven2")
  },
  pomExtra := (
    <scm>
      <url>git@github.com:striation/chain.git</url>
      <connection>scm:git:git@github.com:striation/chain.git</connection>
    </scm>
    <developers>
      <developer>
        <id>striation</id>
        <name>Erik Osheim</name>
        <url>http://github.com/striation/</url>
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
    ReleaseStep(action = Command.process("sonatypeReleaseAll", _)),
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
