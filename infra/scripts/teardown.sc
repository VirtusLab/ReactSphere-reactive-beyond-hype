import $file.common.build
import $file.common.display
import $file.kubernetes.deployments
import $file.kubernetes.tectonic
import $file.common.vars

import deployments._
import display._
import vars._

implicit val progressBar = ProgressBar(System.out, "START", "Starting tear down...")

val appsInParadigm = apps map { _ + s"-$paradigm" }

tearMetricsDown

tearMicroservicesDown(appsInParadigm)

tearCassandraDown

tearDockerRegistryDown

