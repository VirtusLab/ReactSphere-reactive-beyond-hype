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

def performSetup(skipTests: Boolean, skipPublish: Boolean)(implicit stackType: StackType): Unit = {
  implicit val progressBar = ProgressBar(System.out, "START", "Starting Tectonic cluster setup...")
  implicit val env = Prod
  progressBar.start()

  // 1. Check tectonic is online and accessible with kubectl
  verifyKubectlConfiguration

  // 4. Build & test whole stack locally, publish to in-cluster registry
  val appsInParadigm = apps.map { a => s"${a._1}-${stackType.paradigm}" -> a._2 } ++ backingServices
  buildStack(
    projects = appsInParadigm.map(_._1),
    skipTests = skipTests,
    skipPublish = skipPublish,
    registry = Quay
  )

  // 5. Tag nodes
  // # TODO ADD NODE TAGGING HERE

  // 5. Create DNS aliases pointing towards master node with ingress
  requiredAliases foreach createSubdomain

  // 6. Deploy cassandra to Tectonic cluster and wait until it's completely up
  setUpEbsStorageClass()
  deployCassandra
  waitForCassandra

  // 7. Run schema migration job when Cassandra is up
  runCassandraMigration

  // 8. Deploy all microservices and wait for them to be up
  deployAll(appsInParadigm.map(_._1))

  // 9. Deploy monitoring
  deployMetrics
}