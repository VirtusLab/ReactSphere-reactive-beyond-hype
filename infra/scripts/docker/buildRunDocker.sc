
import $file.^.common.display
import $file.^.common.build
import $file.^.common.vars

import build._
import display._
import vars._
import ammonite.ops._
import ammonite.ops.ImplicitWd._
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future, Promise}
import java.util.concurrent.Executors
import scala.concurrent._
import java.util.{Timer, TimerTask}
import scala.util.Try

implicit val ec = new ExecutionContext {
  val threadPool = Executors.newFixedThreadPool(6)

  def execute(runnable: Runnable) { threadPool.submit(runnable) }

  def reportFailure(t: Throwable) { t.printStackTrace() }
}

def performSetup(skipTests: Boolean, skipPublish: Boolean)(implicit stackType: StackType): Unit = {
  implicit val progressBar = ProgressBar(System.out, "START", "Starting setup...")

  progressBar.start()
  progressBar.stepInto("Run docker images...")

  val appsInParadigm = apps.map { a => s"${a._1}-${stackType.paradigm}" -> a._2 } ++ backingServices

  // Tests and publishes sync/async services to local docker registry
  buildPublishApps(apps = appsInParadigm.map(_._1), skipTests = skipTests, skipPublish = skipPublish)

  // Sets up networking shared among containers
  val networkName = setupNetworking()

  // Runs cassandra container
  runCassandra(networkName)

  // Runs apps docker images
  val startedApps = runDockerImages(appsInParadigm, networkName)

  Await.ready(
    startedApps, Duration.Inf
  )

  progressBar.finished()
}

private def buildPublishApps(apps: Seq[String], skipTests: Boolean, skipPublish: Boolean)
                            (implicit progressBar: ProgressBar, stackType: StackType): Unit = {
  buildStack(apps, localRepo = true, skipTests, skipPublish)
}

private def runCassandra(networkName: String)
                           (implicit progressBar: ProgressBar): Unit = {
  progressBar.stepInto("Cassandra")
  progressBar.show("Start up...")

  Future {
    %%docker("run", "--rm", "-v", s"${pwd.toString}/cassandra_data:/var/lib/cassandra", "-p",
      s"${CASSANDRA_PORT}:${CASSANDRA_PORT}", "--network", networkName, "--name", "cassandra", "cassandra:latest")
  }
}

private def runDockerImages(apps: Seq[(String, Int)], networkName: String)
                           (implicit progressBar: ProgressBar, stackType: StackType): Future[Seq[Unit]] = {
  progressBar.stepInto("Running docker images")

  val envVariables = buildEnvVariables

  val startedApps = apps.map { case (app, port) =>
    println(s"Running service: ${app} on port ${port}...")

    val res = %% docker("images", "--filter", s"reference=docker-registry.local/${app}:latest", "--format", "{{.ID}}")
    val imageId = res.out.string.trim
    println(s"Image ID from ${app} is ${imageId}...")

    Future {
      % docker("run", "--rm", "-p", s"${port}:${port}", envVariables, "--name", app, "--network", networkName, imageId)
    }
  }

  delay(10000L) {
    printEndpoints(apps)
  }

  Future.sequence(startedApps)
}

private def setupNetworking()(implicit progressBar: ProgressBar, stackType: StackType): String = {

  progressBar.stepInto("Networking")
  progressBar.show("Setting up...")

  val networkName = "local-bridge"

  val network = %% docker("network", "ls", "--filter", s"name=${networkName}", "--format", "{{.ID}}")
  if(network.out.string.trim.isEmpty) {
    progressBar.show("Network does not exist, creating...")
    % docker("network", "create", networkName)
  }

  networkName
}

private def buildEnvVariables(implicit stackType: StackType): Shellable = {
  Shellable(
    Seq(
      s"BILLING_SERVICE_CONTACT_POINT=${BILLING_APP}-${stackType.paradigm}:${BILLING_PORT}",
      s"IDENTITY_SERVICE_CONTACT_POINT=${IDENTITY_APP}-${stackType.paradigm}:${IDENTITY_PORT}",
      s"PAYMENT_SYSTEM_CONTACT_POINT=${PAYMENT_SYSTEM}:${PAYMENT_PORT}",

      s"CASSANDRA_CONTACT_POINT=${CASSANDRA_HOST}"
    ).flatMap { variable =>
      Seq("-e", variable)
    }
  )
}

private def buildLinks(appsToLink: Seq[String])(implicit stackType: StackType): Shellable = {
  Shellable(
    appsToLink.flatMap { app =>
      Seq("--link", s"${app}:${app}")
    }
  )
}

private def printEndpoints(apps: Seq[(String, Int)])(implicit progressBar: ProgressBar) = {
  val appsToPorts = apps.map { a => s"${a._1}:${a._2}"}.mkString("\n\t")
  println(
    s"""
       |***************************************************
       |Apps are running on ports:
       |\t${appsToPorts}
       |***************************************************
       |""".stripMargin)

  progressBar.show("Started...")
}

private def delay[T](delay: Long)(block: => T): Future[T] = {
  val promise = Promise[T]()
  val t = new Timer()
  t.schedule(new TimerTask {
    override def run(): Unit = {
      promise.complete(Try(block))
    }
  }, delay)
  promise.future
}