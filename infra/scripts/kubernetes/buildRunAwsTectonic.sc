import $file.^.common.display
import $file.^.common.build
import $file.^.common.vars
import $file.^.aws.route53
import $file.tectonic
import $file.deployments

import tectonic._
import deployments._
import vars._
import display._
import build._
import route53._

val requiredAliases = Seq(
  "identity",
  "auctions",
  "billing",
  "payment"
)

// TODO document using $KUBECONFIG
// TODO document cluster deployment and pre-deployment checks

def performSetup(implicit stackType: StackType, steps: StepDefinitions): Unit = {
  implicit val progressBar = ProgressBar(System.out, "START", "Starting Tectonic cluster setup...")
  implicit val env = Prod
  progressBar.start()

  // 1. Check tectonic is online and accessible with kubectl
  verifyKubectlConfiguration

  // 2. Build & test whole stack locally, publish to in-cluster registry
  val apps = appsInParadigm
  buildStack(
    projects = apps.map(_._1),
    publishOpts = PublishOptions(
      sbtTask = Publish, registry = Quay // TODO document Quay sign in
    )
  )

  // 3. Tag nodes
  tagNodes()

  // 4. Create DNS aliases pointing towards master node with ingress
  requiredAliases foreach createSubdomain

  // 5. Deploy cassandra to Tectonic cluster and wait until it's completely up
  setUpEbsStorageClass()
  deployCassandra
  waitForCassandra

  // 6. Run schema migration job when Cassandra is up
  runCassandraMigration

  // 7. Deploy all microservices and wait for them to be up
  deployAll(apps.map(_._1))

  // 8. Deploy monitoring
  deployMetrics
}