//
// Performs setup for local dockerized environment in sync mode
//

import $file.common.splash
import $file.common.vars
import $file.kubernetes.buildRunTectonic

import common.splash._
import common.vars._
import kubernetes.buildRunTectonic._
import ammonite.ops._

@main
def main(stack: String, skipTests: Boolean = false, skipPublish: Boolean = false): Unit = {
  printSplash()
  println(s"Passed params are: stack = ${stack}, skipTests = ${skipTests}, skipPublish = ${skipPublish}")
  performSetup(skipTests, skipPublish)(StackType.fromString(stack))
}