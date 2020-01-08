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
    ws,
    guice,
    "uk.gov.hmrc"         %% "simple-reactivemongo"   % "7.20.0-play-26",
    "com.typesafe.play"   %% "play-ahc-ws-standalone" % "2.0.8",
    "uk.gov.hmrc"         %% "auth-client"            % "2.32.0-play-26",
    "uk.gov.hmrc"         %% "bootstrap-play-26"      % "1.3.0",
    "uk.gov.hmrc"         %% "mongo-lock"             % "6.15.0-play-26",
    "uk.gov.hmrc"         %% "domain"                 % "5.6.0-play-26",
    "org.typelevel"       %% "cats-core"              % "1.6.1",
    "com.typesafe.play"   %% "play-json"              % "2.6.13",
    "org.scalacheck"      %% "scalacheck"             % "1.13.5"
  )

  trait TestDependencies {
    lazy val scope: String = "test"
    def test: Seq[ModuleID]
  }

  object Test {
    def apply() = new TestDependencies {
      override lazy val test = Seq(
        "org.scalatest"           %% "scalatest"          % "3.0.8"             % scope,
        "org.pegdown"             % "pegdown"             % "1.6.0"             % scope,
        "com.typesafe.play"       %% "play-test"          % PlayVersion.current % scope,
        "org.scalatestplus.play"  %% "scalatestplus-play" % "3.1.0"             % "test",
        "org.mockito"             % "mockito-core"        % "2.25.0"            % scope
      )
    }.test
  }

  def apply() = compile ++ Test()
}

