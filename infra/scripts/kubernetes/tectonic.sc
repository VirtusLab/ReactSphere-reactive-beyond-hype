import $file.^.common.display
import $file.^.common.vars

import ammonite.ops._
import display.ProgressBar
import vars._
import scala.util.Try

import scala.language.postfixOps

val tectonicPath = "tectonic-1.7.5"
val tectonicIps = Seq(
  "172.17.4.101",
  "172.17.4.201"
)

def rebootTectonic(implicit progressBar: ProgressBar): Unit = {
  implicit val wd: Path = pwd/tectonicPath
  progressBar stepInto "Tectonic"
  progressBar show "Starting Tectonic Reboot..."
  try {
    progressBar show "Halting Vagrant VMs..."
    %vagrant "halt"
    progressBar show "Starting Vagrant VMs..."
    %vagrant "up"
    progressBar.finishedNamespace()
  } catch {
    case e: Exception =>
      e.printStackTrace()
      progressBar.failed()
      throw e
  }
}

def kubectlCanConnectToTectonicCluster: Boolean = {
  implicit val wd: Path = pwd
  Try {
    %%kubectl("get", "namespaces")
  }.map(_ => true).getOrElse(false)
}

def getVagrantSshKeyPath: String = {
  implicit val wd: Path = pwd/tectonicPath
  val res = %%vagrant "ssh-config"
  res.out.string
    .split("\n")
    .map(_.trim)
    .filter(_ startsWith "IdentityFile")
    .map(_.split(" ")(1))
    .take(1)
    .fold("")(_ + _)
}

def provisionDockerDaemonConfiguration(implicit progressBar: ProgressBar): Unit = {
  implicit val wd: Path = pwd/tectonicPath
  val sshKeyPath = getVagrantSshKeyPath
  progressBar stepInto "Docker"

  try {
    tectonicIps foreach { ipAddress =>
      progressBar show s"Configuring Docker daemon on Tectonic VM: $ipAddress..."
      %scp(
        "-i", sshKeyPath, "-q",
        "-o", "StrictHostKeyChecking=no",
        "-o", "UserKnownHostsFile=/dev/null",
        wd / "provisioning" / "docker" / "daemon.json", s"core@$ipAddress:~"
      )
    }

    tectonicIps foreach { ipAddress =>
      %ssh(
        "-i", sshKeyPath, "-q",
        "-o", "StrictHostKeyChecking=no",
        "-o", "UserKnownHostsFile=/dev/null",
        s"core@$ipAddress",
        "sudo", "mv", "/home/core/daemon.json", "/etc/docker"
      )
    }

    progressBar show "Configured Docker daemons!"
    Thread sleep 300
    progressBar.finishedNamespace()
  } catch {
    case e: Exception =>
      e.printStackTrace()
      progressBar.failed()
      throw e
  }
}

def verifyKubectlConfiguration(implicit progressBar: ProgressBar): Unit = {
  progressBar.stepInto("Verifying kubectl config")

  if (!kubectlCanConnectToTectonicCluster) {
    println("Kubectl can't connect to cluster! Possible problems:\n" +
      " * Is Tectonic up and running?\n" +
      " * Did you obtain kubectl configuration from Tectonic Console?")
    progressBar.failed()
    interp exit 1
  } else {
    println("Tectonic seems to be up & running")
    progressBar.finishedNamespace()
  }
}

def getPodCountInNamespace(namespace: String, label: String, selector: String): Int = {
  implicit val wd: Path = pwd

  val pods = %%kubectl(
    "-n", namespace,
    "get", "pods",
    s"--selector=$label=$selector",
  )

  pods.out.string.split("\n").length - 1 // minus first row which is either 'No resources found.' or column labels
}

def getPodNamesInNamespace(namespace: String, label: String, selector: String): Seq[String] = {
  implicit val wd: Path = pwd

  val pods = %%kubectl(
    "-n", namespace,
    "get", "pods",
    s"--selector=$label=$selector",
    "-o", "jsonpath='{.items[*].metadata.name}'"
  )

  pods.out.string.stripPrefix("'").stripSuffix("'").split(" ")
}

case class Nodes(master: String, workers: Vector[String])

case object MasterNodeIsMissing extends RuntimeException("Tectonic master node is missing!")

def getNodes: Nodes = {
  implicit val wd: Path = pwd
  val nodes = %%kubectl(
    "get", "nodes"
  )

  val nodeLineStrings = nodes.out.string.split("\n").drop(1) // drop labels line
  val nodeLineValues = nodeLineStrings.map(_.split(" ").filter(_ != "").map(_.trim))

  val master = nodeLineValues.find(_(2) == "master").getOrElse(throw MasterNodeIsMissing)(0)
  val workers = nodeLineValues.filter(_(2) == "node").map(_(0))

  Nodes(master, workers.toVector)
}

def tagNodes(): Unit = {
  implicit val wd: Path = pwd

  val nodes: Nodes = getNodes
  val (dbNodes, appNodes) = nodes.workers.splitAt(1) // single node

  dbNodes foreach { node =>
    %kubectl("label", "nodes", node, "nodetype=datastore", "--overwrite=true")
  }

  appNodes foreach { node =>
    %kubectl("label", "nodes", node, "nodetype=microservices", "--overwrite=true")
  }
}

def createNamespace(implicit progressBar: ProgressBar): Unit = {
  progressBar.stepInto("Creating 'microservices' namespace")

  implicit val wd: Path = pwd

  Try(% kubectl("get", "namespace", "microservices")).recover {
    case _ =>
      % kubectl("create", "namespace", "microservices")
  }.get

  progressBar.finishedNamespace()
}

def createAwsCredentials(implicit progressBar: ProgressBar): Unit = {
  progressBar.stepInto("Creating AWS secrets")

  implicit val wd: Path = pwd

  Try(% kubectl("get", "secret", "--namespace", "microservices", K8S_AWS_NAME)).map { _ =>
    % kubectl("delete", "secret", "--namespace", "microservices", K8S_AWS_NAME)
  }.recover {
    case _ =>
      println(s"Secret ${K8S_AWS_NAME} does not exist...")
  }

  println(s"Creating secrets: ${AWS_KEY_K8S_PROP} and ${AWS_SECRET_K8S_PROP}...")
  % kubectl("create", "secret", "generic", "--namespace", "microservices", K8S_AWS_NAME,
    s"--from-literal=${AWS_KEY_K8S_PROP}=${awsKey}",
    s"--from-literal=${AWS_SECRET_K8S_PROP}=${awsSecret}"
  )

  progressBar.finishedNamespace()
}

def createStackParadigmSecret(implicit progressBar: ProgressBar, stackType: StackType): Unit = {
  progressBar stepInto "Creating stack type secret"

  implicit val wd: Path = pwd

  %kubectl("create", "secret", "generic", "--namespace", "microservices", STACK_PARADIGM,
    s"--from-literal=$STACK_PARADIGM=${stackType.paradigm}"
  )

  progressBar.finishedNamespace()
}