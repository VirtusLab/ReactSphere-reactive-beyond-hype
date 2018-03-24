import org.scoverage.coveralls.Imports.CoverallsKeys._
import com.typesafe.sbt.packager.docker.Cmd
import com.typesafe.sbt.packager.docker.DockerPlugin.autoImport.dockerUpdateLatest
import sbt.Keys.resolvers

val installBashCommands = Seq(
  Cmd("USER", "root"),
  Cmd("RUN", "apk", "add", "--update", "bash", "&&", "rm", "-rf", "/var/cache/apk/*"),
  Cmd("USER", "daemon")
)

val ScalatraVersion = "2.6.2"
val AkkaHttpVersion = "10.0.11"
val AkkaVersion     = "2.5.8"

val compileTestScope = "test->test;compile->compile"

lazy val commonSettings = Seq(
  organization        := "com.virtuslab",
  git.baseVersion     := "0.1.0",
  scalaVersion        := "2.12.4",
  dockerBaseImage     := "openjdk:jre-alpine",
  dockerUpdateLatest  := true,
  dockerRepository    := Some(System.getProperty("docker.registry.host", "docker-registry.local")),

  javaOptions in Universal ++= Seq(
    "-J-XX:+UnlockExperimentalVMOptions",
    "-J-XX:+UseCGroupMemoryLimitForHeap",
    "-J-XX:MaxRAMFraction=1",
    "-J-XshowSettings:vm",
    s"-Dservice.version=${version.value}"
  ),

  // enable this when https://github.com/scoverage/sbt-coveralls/pull/110 is merged
  // and 1.2.3 version released
  // coverageEnabled := true,
  // coverallsGitRepoLocation := Some("../"),

  fork in Test := true
)

lazy val commons = (project in file("commons"))
  .settings(
    libraryDependencies ++= Seq(
      "com.github.t3hnar"          %% "scala-bcrypt"             % "3.1",
      // cassandra
      "com.datastax.cassandra"     %  "cassandra-driver-core"    % "3.4.0",
      "com.datastax.cassandra"     %  "cassandra-driver-mapping" % "3.4.0",
      // configuration
      "com.typesafe"               %  "config"                   % "1.3.2",
      // metrics
      "io.prometheus"              %  "simpleclient"             % "0.3.0",
      "io.prometheus"              %  "simpleclient_hotspot"     % "0.3.0",
      "io.prometheus"              %  "simpleclient_httpserver"  % "0.3.0",
      "io.prometheus"              %  "simpleclient_logback"     % "0.3.0",
      // logging
      "org.slf4j"                  %  "slf4j-api"                % "1.7.22",
      "ch.qos.logback"             %  "logback-classic"          % "1.2.3",
      "com.typesafe.scala-logging" %% "scala-logging"            % "3.5.0",
      // testing
      "org.scalatest"              %% "scalatest"                % "3.0.3"    % Test,
      "org.cassandraunit"          %  "cassandra-unit"           % "3.3.0.2"  % Test,
      "log4j"                      %  "log4j"                    % "1.2.17"   % Test
    )
  )

lazy val baseSync = (project in file("base-sync"))
  .settings(
    commonSettings,
    name := "base-sync",
    resolvers += Classpaths.typesafeReleases,
    libraryDependencies ++= Seq(
      "org.scalatra"      %% "scalatra"           % ScalatraVersion,
      "org.scalatra"      %% "scalatra-scalatest" % ScalatraVersion   % "test",
      "org.scalatra"      %% "scalatra-json"      % ScalatraVersion,
      "org.eclipse.jetty" %  "jetty-webapp"       % "9.4.8.v20171121" % "container;compile",
      "org.eclipse.jetty" %  "jetty-plus"         % "9.4.8.v20171121" % "container;compile",
      "javax.servlet"     %  "javax.servlet-api"  % "3.1.0"           % "provided",
      "org.json4s"        %% "json4s-jackson"     % "3.5.2",
      "com.typesafe"      %  "config"             % "1.3.2",
      "org.mockito"       %  "mockito-core"       % "2.15.0"          % Test
    ),
    dockerCommands ++= installBashCommands
  )
  .enablePlugins(ScalatraPlugin, JavaAppPackaging, DockerPlugin, GitVersioning)
  .dependsOn(commons % compileTestScope)

lazy val helloWorldSync = (project in file("hello-world-sync"))
  .settings(
    commonSettings,
    name := "hello-world-sync",
    resolvers += Classpaths.typesafeReleases,
    dockerCommands ++= installBashCommands
  )
  .enablePlugins(ScalatraPlugin, JavaAppPackaging, DockerPlugin, GitVersioning)
  .dependsOn(
    commons % compileTestScope,
    baseSync % compileTestScope
  )

lazy val auctionHousePrimarySync = (project in file("auction-house-primary-sync"))
  .settings(
    commonSettings,
    name := "auction-house-primary-sync",
    resolvers += Classpaths.typesafeReleases,
    dockerCommands ++= installBashCommands
  )
  .enablePlugins(ScalatraPlugin, JavaAppPackaging, DockerPlugin, GitVersioning)
  .dependsOn(
    commons % compileTestScope,
    baseSync % compileTestScope
  )

lazy val auctionHousePrimaryAsync = (project in file("auction-house-primary-async"))
  .settings(
    commonSettings,
    name := "auction-house-primary-async",
    libraryDependencies ++= Seq(
      "com.typesafe.akka"          %% "akka-http"             % AkkaHttpVersion,
      "com.typesafe.akka"          %% "akka-http-spray-json"  % AkkaHttpVersion,
      "com.typesafe.akka"          %% "akka-stream"           % AkkaVersion,
      "com.typesafe.akka"          %% "akka-http-testkit"     % AkkaHttpVersion   % Test,
      "com.typesafe.akka"          %% "akka-testkit"          % AkkaVersion       % Test,
      "com.typesafe.akka"          %% "akka-stream-testkit"   % AkkaVersion       % Test
    ),
    dockerCommands ++= installBashCommands
  )
  .enablePlugins(JavaAppPackaging, DockerPlugin, GitVersioning)
  .dependsOn(commons %"test->test;compile->compile")

lazy val helloWorldAsync = (project in file("hello-world-async"))
  .settings(
    commonSettings,
    name := "hello-world-async",
    libraryDependencies ++= Seq(
      "com.typesafe.akka"          %% "akka-http"            % AkkaHttpVersion,
      "com.typesafe.akka"          %% "akka-http-spray-json" % AkkaHttpVersion,
      "com.typesafe.akka"          %% "akka-stream"          % AkkaVersion,
      "com.typesafe.akka"          %% "akka-http-testkit"    % AkkaHttpVersion   % Test,
      "com.typesafe.akka"          %% "akka-testkit"         % AkkaVersion       % Test,
      "com.typesafe.akka"          %% "akka-stream-testkit"  % AkkaVersion       % Test
    ),
    dockerCommands ++= installBashCommands
  )
  .enablePlugins(JavaAppPackaging, DockerPlugin, GitVersioning)
  .dependsOn(commons % "test->test;compile->compile")

lazy val billingServiceSecondarySync = (project in file("billing-service-secondary-sync"))
    .settings(
      commonSettings,
      name := "billing-service-secondary-sync",
      resolvers += Classpaths.typesafeReleases,
      dockerCommands ++= installBashCommands
    )
    .enablePlugins(ScalatraPlugin, JavaAppPackaging, DockerPlugin, GitVersioning)
  .dependsOn(
    commons % compileTestScope,
    baseSync % compileTestScope
  )

lazy val paymentSystem = (project in file("payment-system"))
  .settings(
    commonSettings,
    name := "payment-system",
    resolvers += Classpaths.typesafeReleases,
    dockerCommands ++= installBashCommands
  )
  .enablePlugins(ScalatraPlugin, JavaAppPackaging, DockerPlugin, GitVersioning)
  .dependsOn(
    commons % compileTestScope,
    baseSync % compileTestScope
  )

lazy val gatlingTests = (project in file("gatling-tests"))
  .settings(
    commonSettings,
    name := "gatling-tests",
    libraryDependencies ++= Seq(
      "io.gatling.highcharts"      %  "gatling-charts-highcharts" % "2.3.0",
      "io.gatling"                 %  "gatling-test-framework"    % "2.3.0",
      "org.json4s"                 %% "json4s-jackson"            % "3.6.0-M2",
      "org.json4s"                 %% "json4s-native"             % "3.6.0-M2",
      "com.typesafe"               %  "config"                    % "1.3.2"
    ),
    dockerCommands ++= installBashCommands
  )
  .enablePlugins(JavaAppPackaging, DockerPlugin, GitVersioning, GatlingPlugin)
  .disablePlugins(CoverallsPlugin)
  .dependsOn(commons % "test->test;compile->compile")

lazy val root = (project in file("."))
  .settings(
    commonSettings,
    name := "beyond-the-hype-codebase",
  )
  .aggregate(
    helloWorldSync, helloWorldAsync,
    baseSync,
    auctionHousePrimarySync, auctionHousePrimaryAsync,
    billingServiceSecondarySync,
    paymentSystem,
    gatlingTests
  )
