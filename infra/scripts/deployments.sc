import java.lang.Thread.sleep

import $file.display
import $file.tectonic
import $file.vars
import ammonite.ops._
import display.ProgressBar
import tectonic._
import vars._

import scala.util.Try

implicit val wd: Path = pwd

def deployDockerRegistry(implicit progressBar: ProgressBar): Unit = {
  progressBar stepInto "Registry"

  if (tectonic.getPodCountInNamespace("docker", "app", "registry") != 1) {
    progressBar show "Deploying registry to cluster..."
    % kubectl("apply", "-f", s"infra/manifests/registry.$env.yaml")

    progressBar show "Waiting for registry to start..."
    sleep(5000)

    if (env == "dev") {
      progressBar show "Setting up docker daemon to trust in-cluster registry..."

      provisionDockerDaemonConfiguration

      rebootTectonic
    }
  } else {
    println("Docker registry already deployed!")
  }

  progressBar stepInto "Registry"
  progressBar.finishedNamespace()
}

def tearDockerRegistryDown(implicit progressBar: ProgressBar): Unit = {
  progressBar stepInto "Registry"

  % kubectl("delete", "--ignore-not-found", "-f", s"infra/manifests/registry.$env.yaml")

  progressBar.finishedNamespace()
}

def waitForDockerRegistry(implicit progressBar: ProgressBar): Unit = {
  progressBar stepInto "Registry"
  progressBar show "Waiting for registry to come online..."

  var retries = 0
  var keepTrying = true

  while (keepTrying) {

    val result = Try {
      %% curl(
        "-k", "-w", "%{http_code}",
        "-s", "-o", "/dev/null",
        "https://docker-registry.local/v2/_catalog" // TODO url configurable
      )
    }

    if (result.isSuccess) keepTrying = false
    else {
      retries += 1
      sleep(2000)
    }

    if (retries > 300) throw new RuntimeException("Docker Registry did not start in expected time frame.")
  }
}

def deployCassandra(implicit progressBar: ProgressBar): Unit = {
  if (tectonic.getPodCountInNamespace("databases", "app", "cassandra") != 2) {
    progressBar.show("Deploying Cassandra")
    % kubectl("apply", "-f", s"infra/manifests/cassandra.$env.yaml")
    println("Deployed Cassandra!")
  } else {
    println("Cassandra already deployed!")
  }
}

def tearCassandraDown(implicit progressBar: ProgressBar): Unit = {
  progressBar stepInto "Cassandra"
  progressBar show "Tearing Cassandra cluster down"

  % kubectl("delete", "--ignore-not-found", "-f", s"infra/manifests/cassandra.$env.yaml")

  progressBar.finishedNamespace()
}

def waitForCassandra(implicit progressBar: ProgressBar): Unit = {
  def cassandraClusterIsReady(output: String, awaitCount: Int): Boolean = {
    val lines = output.split("\n")
      .toIterator
      .drop(5) // 5 first lines are
      .toList

    lines.size == awaitCount && lines.forall(_.startsWith("UN"))
  }

  progressBar.show("Waiting for Cassandra cluster")

  var keepTrying = true
  var retries = 0
  while (keepTrying) {

    val result = Try {
      %% kubectl("-n", "databases", "exec", "cassandra-0", "--", "nodetool", "status")
    }

    if (result.isFailure || !cassandraClusterIsReady(result.get.out.string, 2)) {
      retries += 1
      sleep(2000)
    } else keepTrying = false

    if (retries > 150) throw new RuntimeException("Cassandra cluster did not start in expected time frame.")
  }
}

def runCassandraMigration(implicit progressBar: ProgressBar): Unit = {
  progressBar show "Setting up Cassandra schema"
  % kubectl("apply", "-f", "infra/manifests/migration.cassandra.yaml")
}

def deployAll(apps: Seq[String])(implicit progressBar: ProgressBar): Unit = {
  apps foreach { app =>
    progressBar show s"Deploying $app"
    % kubectl("apply", "-f", s"infra/manifests/$app.$env.yaml")
  }
}

def tearMicroservicesDown(apps: Seq[String])(implicit progressBar: ProgressBar): Unit = {
  progressBar stepInto "Microservices"

  apps foreach { app =>
    progressBar show s"Tearing down $app"
    % kubectl("delete", "--ignore-not-found", "-f", s"infra/manifests/$app.$env.yaml")
  }

  progressBar.finishedNamespace()
}

def deployMetrics(implicit progressBar: ProgressBar): Unit = {
  progressBar show s"Deploying monitoring"
  % kubectl("apply", "-f", s"infra/manifests/monitoring.yaml")
}

def tearMetricsDown(implicit progressBar: ProgressBar): Unit = {
  progressBar show s"Tearing metrics down"
  % kubectl("delete", "--ignore-not-found", "-f", s"infra/manifests/monitoring.yaml")
}

def runLoadTests(implicit progressBar: ProgressBar): Unit = {
  progressBar show s"Running load tests"

  % kubectl("apply", "-f", s"infra/manifests/load-tests.yaml")
}