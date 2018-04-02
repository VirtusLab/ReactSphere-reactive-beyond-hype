import $file.^.common.build
import $file.^.common.display
import $file.^.common.vars
import $file.deployments
import $file.tectonic

import deployments._
import display._
import vars._

def performTeardown(dropInfra: Boolean): Unit = {
  implicit val progressBar = ProgressBar(System.out, "START", "Starting tear down...")

  val appsInParadigm: Seq[String] = Seq(SyncStack, AsyncStack).flatMap { stackType =>
    apps.map { a => s"${a._1}-${stackType.paradigm}" }
  }

  val appsToTeardown = (appsInParadigm ++ backingServices.map(_._1))

  tearMicroservicesDown(appsToTeardown)

  if(dropInfra) {
    tearMetricsDown

    tearCassandraDown

    tearDockerRegistryDown
  }
}

