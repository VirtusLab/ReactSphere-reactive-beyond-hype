//
// Performs setup for local dockerized environment in async or async mode
//

import $file.common.cli
import $file.common.vars
import $file.docker.buildRunDocker

import vars._
import cli._
import buildRunDocker._
import ammonite.ops._

@main
def main(
          stack: String,
          tests: Boolean = true,
          publish: Boolean = true,
          gatling: Boolean = false
        ): Unit = {
  printSplash()
  printParams(stack, tests, publish, gatling)
  performSetup(StackType.fromString(stack), StepDefinitions(tests, publish, gatling))
}
