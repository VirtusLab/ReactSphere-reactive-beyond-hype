// 1. Check tectonic is online and accessible with kubectl
// 2. Deploy docker registry
// 3. Wait until docker registry is available
// 4. Build & test whole stack locally, publish to in-cluster registry
// 5. Deploy cassandra to Tectonic cluster and wait until it's completely up
// 6. Run schema migration job when Cassandra is up
// 7. Deploy all microservices and wait for them to be up
// 8. Deploy monitoring

import $file.common.display
import $file.common.build
import $file.tectonic.tectonic
import $file.tectonic.deployments
import $file.tectonic.vars

import tectonic.tectonic._
import deployments._
import vars._
import display._
import build._

implicit val progressBar = ProgressBar(System.out, "START", "Starting setup...")
progressBar.start()

// 1. Check tectonic is online and accessible with kubectl
verifyKubectlConfiguration

// 2. Deploy docker registry
deployDockerRegistry

// 3. Wait until docker registry is available
waitForDockerRegistry

// 4. Build & test whole stack locally, publish to in-cluster registry
val appsInParadigm = apps map { _ + s"-$paradigm" }
buildStack(appsInParadigm)

// 5. Deploy cassandra to Tectonic cluster and wait until it's completely up
deployCassandra
waitForCassandra

// 6. Run schema migration job when Cassandra is up
runCassandraMigration

// 7. Deploy all microservices and wait for them to be up
deployAll(appsInParadigm)

// 8. Deploy monitoring
deployMetrics