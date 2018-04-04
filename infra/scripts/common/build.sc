import $file.display

import ammonite.ops._
import display.ProgressBar
import $file.vars
import vars._

private def directoryToProjectName(directoryName: String) = {
  val segments = directoryName.split("-")
  segments.head + segments.tail.map(_.capitalize).mkString
}

def buildStack(projects: Seq[String], registry: Registry,
               localRepo: Boolean = false, skipTests: Boolean = true, skipPublish: Boolean = true)
              (implicit progressBar: ProgressBar): Unit = {
  implicit val codebasePath = pwd / "codebase"
  progressBar stepInto "Build"

  projects foreach { directory =>
    val project = directoryToProjectName(directory)

    if(!skipTests) {
      progressBar show s"Testing $project..."
      % sbt(s"-Ddocker.registry.host=${registry.value}", "coverageOff", s"$project/test")
    }

    if(!skipPublish) {
      progressBar show s"Publishing $project..."
      % sbt(s"-Ddocker.registry.host=${registry.value}", "coverageOff", s"$project/docker:${publishTask(localRepo)}")
    }
  }

  progressBar.finishedNamespace()
}

private def publishTask(localRepo: Boolean) = if (localRepo) "publishLocal" else "publish"
