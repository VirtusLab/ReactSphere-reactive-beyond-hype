//
// Performs setup for local dockerized environment in async mode
//

import $file.docker.vars
import $file.docker.buildRunDocker

import docker.buildRunDocker._
import docker.vars._

performSetup(AsyncStack)
