import $file.^.common.display
import $file.^.common.build
import $file.^.common.vars
import $file.tectonic
import $file.deployments

import tectonic._
import deployments._
import vars._
import display._
import build._


def performSetup(implicit stackType: StackType, steps: StepDefinitions): Unit = {
  implicit val progressBar = ProgressBar(System.out, "START", "Starting Tectonic cluster setup...")
  implicit val env = Dev
  progressBar.start()

  // 1. Check tectonic is online and accessible with kubectl
  verifyKubectlConfiguration

  // 2. Deploy docker registry
  deployDockerRegistry

  // 3. Wait until docker registry is available
  waitForDockerRegistry

  // 4. Build & test whole stack locally, publish to in-cluster registry
  val apps = appsInParadigm
  buildStack(
    projects = apps.map(_._1),
    publishOpts = PublishOptions(
      sbtTask = Publish, registry = Local
    )
  )

  // 5. Deploy cassandra to Tectonic cluster and wait until it's completely up
  deployCassandra
  waitForCassandra

  // 6. Run schema migration job when Cassandra is up
  runCassandraMigration

  // 7. Deploy all microservices and wait for them to be up
  deployAll(apps.map(_._1))

  // 8. Deploy monitoring
  deployMetrics
}