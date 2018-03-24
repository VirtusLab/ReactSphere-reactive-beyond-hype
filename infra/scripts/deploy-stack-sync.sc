import $file.build
import $file.deployments
import $file.display
import $file.tectonic
import $file.vars
import build._
import deployments.deployAll
import display.ProgressBar
import tectonic._

implicit val progressBar = ProgressBar(System.out, "", "Preparing...")
progressBar.start()

val projects = vars.apps map { _ + "-sync" }

buildStack(projects)

progressBar stepInto "Kubectl"
progressBar show "Verifying kubectl config..."

verifyKubectlConfiguration

progressBar show "Deploying..."
deployAll(projects)

progressBar.finishedNamespace()
progressBar.finished()