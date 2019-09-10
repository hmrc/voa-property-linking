import sbt._

object MicroServiceBuild extends Build with MicroService {

  val appName = "voa-property-linking"

  override val defaultPort: Int = 9524

  override lazy val appDependencies: Seq[ModuleID] = AppDependencies()
}

private object AppDependencies {

  import play.core.PlayVersion
  import play.sbt.PlayImport._

  val compile = Seq(
    "uk.gov.hmrc" %% "play-reactivemongo" % "5.2.0",
    ws,
    "ai.x" %% "play-json-extensions" % "0.9.0",
    "uk.gov.hmrc" %% "auth-client" % "2.5.0",
    "uk.gov.hmrc" %% "bootstrap-play-25" % "4.13.0",
    "uk.gov.hmrc" %% "mongo-lock" % "5.1.1",
    "uk.gov.hmrc" %% "domain" % "4.1.0",
    "org.typelevel" %% "cats-core" % "1.0.1",
    "com.google.inject.extensions" % "guice-multibindings" % "4.0",
    "uk.gov.hmrc" %% "reactive-circuit-breaker" % "2.1.0"
  )

  trait TestDependencies {
    lazy val scope: String = "test"
    def test: Seq[ModuleID]
  }

  object Test {
    def apply() = new TestDependencies {
      override lazy val test = Seq(
        "uk.gov.hmrc" %% "hmrctest" % "3.3.0" % scope,
        "uk.gov.hmrc" %% "reactivemongo-test" % "2.0.0" % scope,
        "org.scalatest" %% "scalatest" % "3.0.6" % scope,
        "org.pegdown" % "pegdown" % "1.6.0" % scope,
        "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
        "com.github.tomakehurst" % "wiremock" % "2.5.1" % scope,
        "org.mockito" % "mockito-core" % "2.25.0" % scope
      )
    }.test
  }

  def apply() = compile ++ Test()
}

