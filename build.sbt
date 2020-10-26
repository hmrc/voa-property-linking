import _root_.play.core.PlayVersion
import play.sbt.PlayImport._
import play.sbt.routes.RoutesKeys
import sbt.Keys._
import sbt.Tests.{Group, SubProcess}
import sbt._
import uk.gov.hmrc.DefaultBuildSettings._
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin.publishingSettings
import uk.gov.hmrc.versioning.SbtGitVersioning
import uk.gov.hmrc.{SbtArtifactory, SbtAutoBuildPlugin}

lazy val appName = "voa-property-linking"

lazy val playSettings: Seq[Setting[_]] = Seq(
  RoutesKeys.routesImport ++= Seq(
    "uk.gov.hmrc.voapropertylinking.binders.propertylinks._",
    "uk.gov.hmrc.voapropertylinking.binders.clients._",
    "uk.gov.hmrc.voapropertylinking.binders.propertylinks.temp._",
    "scala.language.reflectiveCalls",
    "models._"
  )
)

val defaultPort = 9524

lazy val scoverageSettings: Seq[Def.Setting[_ >: String with Double with Boolean]] = {
  // Semicolon-separated list of regexs matching classes to exclude
  import scoverage.ScoverageKeys
  Seq(
    ScoverageKeys.coverageExcludedPackages := "<empty>;.*Reverse.*;Reverse.*;views.*;uk.gov.hmrc.voapropertylinking.config.*;.*\\.temp\\..*;.*\\.test\\..*;poc.view.*;poc.uk.gov.hmrc.voapropertylinking.config.*;.*(AuthService|BuildInfo|Routes).*",
    ScoverageKeys.coverageMinimum := 75,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true
  )
}

lazy val microservice: Project = Project(appName, file("."))
  .enablePlugins(PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin, SbtArtifactory)
  .disablePlugins(JUnitXmlReportPlugin)
  .settings(playSettings: _*)
  .settings(scoverageSettings: _*)
  .settings(scalaSettings: _*)
  .settings(publishingSettings: _*)
  .settings(defaultSettings(): _*)
  .settings(PlayKeys.playDefaultPort := defaultPort)
  .settings(majorVersion := 0)
  .settings(
    libraryDependencies ++= compileDependencies ++ testDependencies,
    libraryDependencies ++= Seq(
      compilerPlugin("com.github.ghik" % "silencer-plugin" % "1.7.0" cross CrossVersion.full),
      "com.github.ghik" % "silencer-lib" % "1.7.0" % Provided cross CrossVersion.full
    ),
    retrieveManaged := true,
    evictionWarningOptions in update := EvictionWarningOptions.default.withWarnScalaVersionEviction(false),
    testGrouping in Test := oneForkedJvmPerTest((definedTests in Test).value)
  )
  .settings(scalaVersion := "2.12.12")

def oneForkedJvmPerTest(tests: Seq[TestDefinition]) = tests.map { test =>
  Group(test.name, Seq(test), SubProcess(ForkOptions().withRunJVMOptions(Vector(s"-Dtest.name=${test.name}"))))
}

scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature")

// silence all warnings on autogenerated files
scalacOptions += "-P:silencer:pathFilters=target/.*"
// Make sure you only exclude warnings for the project directories, i.e. make builds reproducible
scalacOptions += s"-P:silencer:sourceRoots=${baseDirectory.value.getCanonicalPath}"

val compileDependencies = Seq(
  ws,
  guice,
  "uk.gov.hmrc"        %% "bootstrap-backend-play-26" % "2.23.0",
  "uk.gov.hmrc"        %% "simple-reactivemongo"      % "7.20.0-play-26",
  "uk.gov.hmrc"        %% "auth-client"               % "2.32.0-play-26",
  "uk.gov.hmrc"        %% "mongo-lock"                % "6.15.0-play-26",
  "uk.gov.hmrc"        %% "domain"                    % "5.6.0-play-26",
  "org.typelevel"      %% "cats-core"                 % "1.6.1",
  "com.typesafe.play"  %% "play-json"                 % "2.6.13",
  "org.scalacheck"     %% "scalacheck"                % "1.13.5",
  "uk.gov.hmrc"        %% "uri-template"              % "1.3.0",
  "org.apache.commons" %  "commons-text"              % "1.6"
)

val testDependencies = Seq(
  "org.scalatest"          %% "scalatest"           % "3.0.8"             % "test",
  "org.pegdown"            %  "pegdown"             % "1.6.0"             % "test",
  "com.typesafe.play"      %% "play-test"           % PlayVersion.current % "test",
  "org.scalatestplus.play" %% "scalatestplus-play"  % "3.1.0"             % "test",
  "org.mockito"            %  "mockito-core"        % "2.25.0"            % "test"
)

addCommandAlias("precommit", ";scalafmt;test:scalafmt;coverage;test;coverageReport")
