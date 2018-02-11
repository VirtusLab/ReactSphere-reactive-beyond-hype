import ammonite.ops.ImplicitWd.implicitCwd
import ammonite.ops._
import $file.tectonic
import $file.display
import $file.stack
import stack._
import tectonic._
import display.ProgressBar

val projects = Seq(
  "hello-world"
)

val suffix = "async"

implicit val progressBar = ProgressBar(System.out, "", "Preparing...")
progressBar.start()

buildStack(projects, suffix)

progressBar stepInto "Kubectl"
progressBar show "Verifying kubectl config..."

verifyKubectlConfiguration

progressBar show "Deploying..."
%kubectl("apply", "-f", "infra/manifests/hello-world-async.dev.yaml") // TODO [ENVIRONMENTS]

progressBar.finishedNamespace()
progressBar.finished()