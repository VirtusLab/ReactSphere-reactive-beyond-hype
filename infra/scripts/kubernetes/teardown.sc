import $file.^.common.build
import $file.^.common.display
import $file.^.common.vars
import $file.deployments
import $file.tectonic

import deployments._
import display._
import vars._
import build._

def performTeardown(dropInfra: Boolean): Unit = {
  implicit val env = Dev
  implicit val progressBar = ProgressBar(System.out, "START", "Starting tear down...")

  val steps = StepDefinitions(
    gatling = true
  )

  val apps: Seq[String] = (
    appsInParadigm(SyncStack, steps) ++ appsInParadigm(AsyncStack, steps)
    )
    .map(_._1).toSet.toList

  tearMicroservicesDown(apps)

  if(dropInfra) {
    tearMetricsDown

    tearCassandraDown

    tearDockerRegistryDown
  }
}

