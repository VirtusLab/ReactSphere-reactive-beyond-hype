import $file.^.common.build
import $file.^.common.display
import $file.^.common.vars
import $file.deployments
import $file.tectonic
import build._
import deployments._
import display._
import vars._

def performTeardown(dropInfra: Boolean): Unit = {
  implicit val env = Prod
  implicit val progressBar = ProgressBar(System.out, "START", "Starting tear down...")

  val steps = StepDefinitions(
    gatling = true
  )

  val apps: Seq[String] =
    (appsInParadigm(SyncStack, steps) ++ appsInParadigm(AsyncStack, steps))
      .map(_._1)
      .toSet
      .toList

  tearMicroservicesDown(apps)

  if (dropInfra) {
    tearMetricsDown

    tearCassandraDown
  }
}

