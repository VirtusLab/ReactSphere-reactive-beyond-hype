import $file.display
import ammonite.ops._
import display.ProgressBar

def directoryToProjectName(directoryName: String) = {
  val segments = directoryName.split("-")
  segments.head + segments.tail.map(_.capitalize).mkString
}

def buildStack(projects: Seq[String])(implicit progressBar: ProgressBar): Unit = {
  implicit val codebasePath = pwd / "codebase"
  progressBar stepInto "Build"

  projects foreach { directory =>
    val project = directoryToProjectName(directory)
    progressBar show s"Testing..."
    % sbt("coverageOff", s"$project/test")

    progressBar show s"Publishing..."
    % sbt("coverageOff", s"$project/docker:publish")
  }
}