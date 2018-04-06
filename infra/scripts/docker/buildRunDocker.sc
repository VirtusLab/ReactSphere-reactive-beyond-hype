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

def performSetup(implicit stackType: StackType, steps: StepDefinitions): Unit = {
  implicit val progressBar = ProgressBar(System.out, "START", "Starting setup...")

  progressBar.start()
  progressBar.stepInto("Run docker images...")

  val apps = appsInParadigm

  // Tests and publishes sync/async services to local docker registry
  buildPublishApps(apps = apps.map(_._1))

  // Sets up networking shared among containers
  val networkName = setupNetworking()

  // Runs cassandra container
  runCassandra(networkName)

  // Runs apps docker images
  val startedApps = runDockerImages(apps, networkName)

  Await.ready(
    startedApps, Duration.Inf
  )

  progressBar.finished()
}

private def buildPublishApps(apps: Seq[String])
                            (implicit progressBar: ProgressBar, stackType: StackType, steps: StepDefinitions): Unit = {
  buildStack(
    apps,
    publishOpts = PublishOptions(
      sbtTask = PublishLocal, registry = Local
    )
  )
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

  val startedApps = apps.map { case (app, port) =>
    println(s"Running service: ${app} on port ${port}...")

    val envVariables = buildEnvVariables(app)

    val res = %% docker("images", "--filter", s"reference=docker-registry.local/${app}:latest", "--format", "{{.ID}}")
    val imageId = res.out.string.trim
    println(s"Image ID from ${app} is ${imageId}...")

    Future {
      val params = Seq(
        Seq("run", "--rm"),
        if(port > 0) Seq("-p", s"${port}:${port}") else Nil,
        envVariables,
        Seq("--name", app),
        Seq("--network", networkName),
        Seq(imageId)
      )
      % docker(Shellable(params.flatten))
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

private def buildEnvVariables(app: String)(implicit stackType: StackType): Seq[String] = {
  val specificVars = app match {
    case a if a == s"${BILLING_APP}-${stackType.paradigm}" =>
      Seq(
        s"AWS_ACCESS_KEY_ID=${Option(System.getenv("BILLING_WORKER_AWS_KEY")).getOrElse("AWS_ACCESS_KEY_NOT_SET")}",
        s"AWS_SECRET_ACCESS_KEY=${Option(System.getenv("BILLING_WORKER_AWS_SECRET")).getOrElse("AWS_SECRET_NOT_SET")}"
      )
    case a if a == s"${gatling}" =>
      Seq(
        s"GATLING_DELAY=${GATLING_DELAY}"
      )
    case _ =>
      Nil
  }

  (defaultEnvVariables ++ specificVars).flatMap { variable =>
    Seq("-e", variable)
  }
}

private def defaultEnvVariables(implicit stackType: StackType): Seq[String] = Seq(
  s"AUCTION_SERVICE_CONTACT_POINT=${AUCTION_APP}-${stackType.paradigm}:${AUCTION_PORT}",
  s"BILLING_SERVICE_CONTACT_POINT=${BILLING_APP}-${stackType.paradigm}:${BILLING_PORT}",
  s"IDENTITY_SERVICE_CONTACT_POINT=${IDENTITY_APP}-${stackType.paradigm}:${IDENTITY_PORT}",
  s"PAYMENT_SYSTEM_CONTACT_POINT=${PAYMENT_SYSTEM}:${PAYMENT_PORT}",

  s"CASSANDRA_CONTACT_POINT=${CASSANDRA_HOST}"
)

private def buildLinks(appsToLink: Seq[String])(implicit stackType: StackType): Shellable = {
  Shellable(
    appsToLink.flatMap { app =>
      Seq("--link", s"${app}:${app}")
    }
  )
}

private def printEndpoints(apps: Seq[(String, Int)])(implicit progressBar: ProgressBar) = {
  val appsToPorts = apps.map {
    case (app, port) if port <= 0 => s"${app}:<no port exposed>"
    case (app, port) => s"${app}:${port}"
  }.mkString("\n\t")

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