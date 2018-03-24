import $file.build
import $file.deployments
import $file.display
import $file.tectonic
import $file.vars

import deployments._
import display._
import vars._

implicit val progressBar = ProgressBar(System.out, "START", "Starting tear down...")

val appsInParadigm = apps map { _ + s"-$paradigm" }

tearMetricsDown

tearMicroservicesDown(appsInParadigm)

tearCassandraDown

tearDockerRegistryDown

