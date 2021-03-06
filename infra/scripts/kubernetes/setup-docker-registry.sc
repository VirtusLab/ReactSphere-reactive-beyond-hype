import $file.^.common.display
import $file.tectonic
import $file.deployments

import tectonic._
import deployments._
import display.ProgressBar

implicit val progressBar = ProgressBar(System.out, "Registry", "Checking kubectl...")
progressBar.start()

verifyKubectlConfiguration

deployDockerRegistry

progressBar.finished()
