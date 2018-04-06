//
// Performs setup for local dockerized environment in sync mode
//

import $file.common.cli
import $file.common.vars
import $file.kubernetes.buildRunTectonic
import buildRunTectonic._
import cli._
import vars._

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