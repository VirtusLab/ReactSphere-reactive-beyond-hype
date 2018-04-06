import $file.display

import ammonite.ops._
import display.ProgressBar
import $file.vars
import vars._

private def directoryToProjectName(directoryName: String) = {
  val segments = directoryName.split("-")
  segments.head + segments.tail.map(_.capitalize).mkString
}

def appsInParadigm(implicit stackType: StackType, steps: StepDefinitions): Seq[(String, Int)] = {
  apps.map { a =>
    s"${a._1}-${stackType.paradigm}" -> a._2
  } ++
  backingServices ++
  ( if(steps.gattling) Seq(gattling -> -1) else Nil )
}

def buildStack(projects: Seq[String], publishOpts: PublishOptions)
              (implicit progressBar: ProgressBar, steps: StepDefinitions): Unit = {
  implicit val codebasePath = pwd / "codebase"
  progressBar.stepInto("Build")
  progressBar.show("SBT build in progress..")

  val sbtParams = projects.map { directory: String =>
    val project = directoryToProjectName(directory)

    Seq(
      if(steps.tests) Option(s"$project/test") else None,

      if(steps.publish) {
        val task = publishOpts.sbtTask
        Option(s"$project/docker:${task.name}")
      } else {
        None
      }
    ).flatten
  }.flatten

  val registry = publishOpts.registry
  val params = Seq(s"-Ddocker.registry.host=${registry.value}", "coverageOff") ++ sbtParams
  % sbt(Shellable(params))

  progressBar.finishedNamespace()
}
