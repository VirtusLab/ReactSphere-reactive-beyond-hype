//
// Performs setup for local dockerized environment in async or async mode
//

import $file.common.splash
import $file.common.vars
import $file.docker.buildRunDocker

import vars._
import splash._
import buildRunDocker._
import ammonite.ops._

@main
def main(stack: String, skipTests: Boolean = false, skipPublish: Boolean = false): Unit = {
  printSplash()
  println(s"Passed params are: stack = $stack, skipTests = $skipTests, skipPublish = $skipPublish")
  performSetup(skipTests, skipPublish)(StackType.fromString(stack))
}