import sbt.*

object AppDependencies {

  private val bootstrapVersion = "10.8.0"
  

  val compile = Seq(
    "uk.gov.hmrc"             %% "bootstrap-backend-play-30"  % bootstrapVersion
  )

  val test = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-30"     % bootstrapVersion            % Test,
    "org.scalacheck"          %% "scalacheck"                 % "1.18.1"                    % Test
  )

  val it = Seq.empty
}
