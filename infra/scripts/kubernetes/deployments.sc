import java.lang.Thread.sleep

import $file.^.common.display
import $file.^.common.vars
import $file.tectonic
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
    % kubectl("apply", "-f", s"infra/manifests/registry.dev.yaml")

    progressBar show "Waiting for registry to start..."
    sleep(5000)

    progressBar show "Setting up docker daemon to trust in-cluster registry..."

    provisionDockerDaemonConfiguration

    rebootTectonic
  } else {
    println("Docker registry already deployed!")
  }

  progressBar stepInto "Registry"
  progressBar.finishedNamespace()
}

def tearDockerRegistryDown(implicit progressBar: ProgressBar): Unit = {
  progressBar stepInto "Registry"

  % kubectl("delete", "--ignore-not-found", "-f", s"infra/manifests/registry.dev.yaml")

  progressBar.finishedNamespace()
}

def waitForDockerRegistry(implicit progressBar: ProgressBar): Unit = {
  progressBar stepInto "Docker registry response"
  progressBar show "Waiting for registry to come online..."

  val success = Stream.continually {

    val result = Try {
      %% curl(
        "-k", "-w", "%{http_code}",
        "-s", "-o", "/dev/null",
        "https://docker-registry.local/v2/_catalog"
      )
    }

    result.isSuccess
  }.zip {
    Stream
      .iterate(0)(_ + 1)
  }.takeWhile { case (isSuccess, retries) =>
    !(isSuccess || retries > 300)
  }.foldLeft(true) { case (isSuccess, _) =>
    isSuccess
  }

  if (!success) {
    throw new RuntimeException("Docker Registry did not start in expected time frame.")
  } else {
    progressBar.finishedNamespace()
  }
}

def setUpEbsStorageClass(): Unit = {
  % kubectl("apply", "-f", s"infra/manifests/ebs-storageclass.prod.yaml")
}

def deployCassandra(implicit env: Environment, progressBar: ProgressBar): Unit = {
  if (tectonic.getPodCountInNamespace("databases", "app", "cassandra") != 2) {
    progressBar.show("Deploying Cassandra")
    % kubectl("apply", "-f", s"infra/manifests/cassandra.${env.name}.yaml")
    println("Deployed Cassandra!")
  } else {
    println("Cassandra already deployed!")
  }
}

def tearCassandraDown(implicit env: Environment, progressBar: ProgressBar): Unit = {
  progressBar stepInto "Cassandra"
  progressBar show "Tearing Cassandra cluster down"

  % kubectl("delete", "--ignore-not-found", "-f", s"infra/manifests/cassandra.${env.name}.yaml")

  progressBar.finishedNamespace()
}

def waitForCassandra(implicit progressBar: ProgressBar): Unit = {
  progressBar.stepInto("Waiting for Cassandra")

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

    if (retries > 150) {
      throw new RuntimeException("Cassandra cluster did not start in expected time frame.")
    }
  }

  progressBar.finishedNamespace()
}

def runCassandraMigration(implicit progressBar: ProgressBar): Unit = {
  progressBar.stepInto("Cassandra migration")
  progressBar show "Setting up Cassandra schema"
  % kubectl("apply", "-f", "infra/manifests/migration.cassandra.yaml")
  progressBar.finishedNamespace()
}

def deployAll(apps: Seq[String])(implicit env: Environment, progressBar: ProgressBar): Unit = {
  apps foreach { app =>
    progressBar.stepInto(s"Deploying app: $app")
    println(s"Deploying app: $app...")
    % kubectl("apply", "-f", s"infra/manifests/$app.${env.name}.yaml")
    progressBar.finishedNamespace()
  }
}

def tearMicroservicesDown(apps: Seq[String])(implicit env: Environment, progressBar: ProgressBar): Unit = {
  progressBar stepInto "Microservices"

  apps foreach { app =>
    progressBar show s"Tearing down $app"
    Try {
      % kubectl("delete", "--ignore-not-found", "-f", s"infra/manifests/$app.${env.name}.yaml")
    }.recover {
      case e => println("Error while deleting app but we continue further...")
    }

    println(s"Application $app destroyed")
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