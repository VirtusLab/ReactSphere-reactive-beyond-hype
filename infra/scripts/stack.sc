import ammonite.ops._
import $file.display
import display.ProgressBar

def directoryToProjectName(directoryName: String) = {
  val segments = directoryName.split("-")
  segments.head + segments.tail.map(_.capitalize).mkString
}

def buildStack(projects: Seq[String], suffix: String)(implicit progressBar: ProgressBar) = {
  implicit val codebasePath = pwd/"codebase"
  progressBar stepInto "Build"

  projects map { _ + s"-$suffix" } foreach { directory =>
    val project = directoryToProjectName(directory)
    progressBar show s"Testing..."
    %sbt("project", s"$project/test")

    progressBar show s"Publishing..."
    %sbt("project", s"$project/docker:publish")
  }
}