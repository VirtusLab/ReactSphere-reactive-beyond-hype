import $file.display

import ammonite.ops._
import display.ProgressBar

private def directoryToProjectName(directoryName: String) = {
  val segments = directoryName.split("-")
  segments.head + segments.tail.map(_.capitalize).mkString
}

def buildStack(projects: Seq[String], localRepo: Boolean = false, test: Boolean = true)
              (implicit progressBar: ProgressBar): Unit = {
  implicit val codebasePath = pwd / "codebase"
  progressBar stepInto "Build"

  projects foreach { directory =>
    val project = directoryToProjectName(directory)

    if(test) {
      progressBar show s"Testing ${project}..."
      % sbt("coverageOff", s"$project/test")
    }

    progressBar show s"Publishing ${project}..."
    % sbt("coverageOff", s"$project/docker:${publishTask(localRepo)}")
  }
}

private def publishTask(localRepo: Boolean) = if(localRepo) "publishLocal" else "publish"
