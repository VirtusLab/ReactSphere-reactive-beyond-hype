//
// Performs setup for AWS Tectonic environment in sync mode
//

import $file.common.cli
import $file.common.vars
import $file.kubernetes.buildRunAwsTectonic
import buildRunAwsTectonic._
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