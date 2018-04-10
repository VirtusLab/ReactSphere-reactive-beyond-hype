//
// Tears down all the services and infra from kubernetes cluster
//

import $file.common.cli
import $file.kubernetes.teardown

import cli._
import teardown._
import ammonite.ops._

@main
def main(dropInfra: Boolean = false): Unit = {
  printSplash()
  println(s"Passed params are: dropInfra = ${dropInfra}")
  performTeardown(dropInfra)
}