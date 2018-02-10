import ammonite.ops._
import $file.display
import display.ProgressBar
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