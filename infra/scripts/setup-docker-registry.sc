import ammonite.ops.ImplicitWd.implicitCwd
import ammonite.ops._
import $file.tectonic
import $file.display
import tectonic._
import display.ProgressBar

implicit val progressBar = ProgressBar(System.out, "Registry", "Checking kubectl...")
progressBar.start()

if (!kubectlCanConnectToTectonicCluster) {
  println("Kubectl can't connect to cluster! Possible problems:\n" +
    " * Is Tectonic up and running?\n" +
    " * Did you obtain kubectl configuration from Tectonic Console?")
  progressBar.failed()
  interp exit 1
}

progressBar show "Deploying registry to cluster..."
%kubectl("apply", "-f", "infra/manifests/registry.dev.yaml") // TODO [ENVIRONMENTS]


progressBar show "Waiting for registry to start..."
Thread sleep 5000

progressBar show "Setting up docker daemon to trust in-cluster registry..."

provisionDockerDaemonConfiguration

rebootTectonic

progressBar.finished()
