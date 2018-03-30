import $file.common.build
import $file.common.display
import $file.tectonic.deployments
import $file.tectonic.tectonic
import $file.tectonic.vars

import build._
import deployments.deployAll
import display.ProgressBar
import tectonic._

implicit val progressBar = ProgressBar(System.out, "", "Preparing...")
progressBar.start()

val projects = vars.apps map { _ + "-async" }

buildStack(projects)

progressBar stepInto "Kubectl"
progressBar show "Verifying kubectl config..."

verifyKubectlConfiguration

progressBar show "Deploying..."
deployAll(projects)

progressBar.finishedNamespace()
progressBar.finished()