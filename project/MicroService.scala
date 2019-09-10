import org.scalastyle.sbt.ScalastylePlugin
import play.routes.compiler.StaticRoutesGenerator
import play.sbt.PlayImport.PlayKeys
import play.sbt.PlayScala
import play.sbt.routes.RoutesKeys
import play.sbt.routes.RoutesKeys.routesGenerator
import sbt.Keys._
import sbt.Tests.{Group, SubProcess}
import sbt.{Def, _}
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin._
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion

trait MicroService {

  import uk.gov.hmrc._
  import DefaultBuildSettings._
  import uk.gov.hmrc.SbtAutoBuildPlugin
  import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin
  import uk.gov.hmrc.versioning.SbtGitVersioning

  val appName: String

  lazy val appDependencies: Seq[ModuleID] = ???
  lazy val plugins: Seq[Plugins] = Seq.empty
  lazy val playSettings: Seq[Setting[_]] = Seq(
    RoutesKeys.routesImport ++= Seq(
      "binders.propertylinks._",
      "binders.propertylinks.temp._",
      "scala.language.reflectiveCalls",
      "models._"
    )
  )

  lazy val compileScalastyle = taskKey[Unit]("compileScalastyle")

  val defaultPort = 9000

  lazy val scoverageSettings: Seq[Def.Setting[_ >: String with Double with Boolean]] = {
    // Semicolon-separated list of regexs matching classes to exclude
    import scoverage.ScoverageKeys
    Seq(
      ScoverageKeys.coverageExcludedPackages := "<empty>;Reverse.*;views.*;uk.gov.hmrc.voapropertylinking.config.*;connectors.test.*;controllers.test.*;poc.view.*;poc.uk.gov.hmrc.voapropertylinking.config.*;.*(AuthService|BuildInfo|Routes).*",
      ScoverageKeys.coverageMinimum := 50,
      ScoverageKeys.coverageFailOnMinimum := false,
      ScoverageKeys.coverageHighlighting := true
    )
  }

  lazy val microservice: Project = Project(appName, file("."))
    .enablePlugins(Seq(PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin, SbtArtifactory) ++ plugins: _*)
    .settings(playSettings ++ scoverageSettings: _*)
    .settings(scalaSettings: _*)
    .settings(publishingSettings: _*)
    .settings(defaultSettings(): _*)
    .settings(PlayKeys.playDefaultPort := defaultPort)
    .settings(ScalastylePlugin.scalastyleFailOnError := false)
    .settings(majorVersion := 0)
    .settings(
      libraryDependencies ++= appDependencies,
      retrieveManaged := true,
      evictionWarningOptions in update := EvictionWarningOptions.default.withWarnScalaVersionEviction(false),
      routesGenerator := StaticRoutesGenerator,
      compileScalastyle := org.scalastyle.sbt.ScalastylePlugin.scalastyle.in(Compile).toTask("").value,
      testGrouping in Test := TestPhases.oneForkedJvmPerTest((definedTests in Test).value),
      test in Test <<= (test in Test) dependsOn compileScalastyle
    )
}

private object TestPhases {

  def oneForkedJvmPerTest(tests: Seq[TestDefinition]): Seq[Group] =
    tests map {
      test => Group(test.name, Seq(test), SubProcess(ForkOptions(runJVMOptions = Seq("-Dtest.name=" + test.name))))
    }
}
