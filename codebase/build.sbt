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

//  coverageEnabled := true,
  coverallsGitRepoLocation := Some("../"),

  fork in Test := true
)

lazy val containerSettings = Seq(
  dockerBaseImage     := "openjdk:jre-alpine",
  dockerUpdateLatest  := true,
  dockerRepository    := Some(System.getProperty("docker.registry.host", "docker-registry.local")),

  javaOptions in Universal ++= Seq(
    "-J-XX:+UnlockExperimentalVMOptions",
    "-J-XX:+UseCGroupMemoryLimitForHeap",
    "-J-XX:MaxRAMFraction=1",
    "-J-XshowSettings:vm",
    s"-Dservice.version=${version.value}"
  )
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
      "org.scalaj"        %% "scalaj-http"        % "2.3.0",
      "org.mockito"       %  "mockito-core"       % "2.15.0"          % Test
    )
  )
  .enablePlugins(ScalatraPlugin, GitVersioning)
  .dependsOn(commons % compileTestScope)

lazy val baseAsync = (project in file("base-async"))
  .settings(
    commonSettings,
    name := "base-async",
    libraryDependencies ++= Seq(
      "com.typesafe.akka"          %% "akka-http"             % AkkaHttpVersion,
      "com.typesafe.akka"          %% "akka-http-spray-json"  % AkkaHttpVersion,
      "com.typesafe.akka"          %% "akka-stream"           % AkkaVersion,
      "com.typesafe.akka"          %% "akka-http-testkit"     % AkkaHttpVersion   % Test,
      "com.typesafe.akka"          %% "akka-testkit"          % AkkaVersion       % Test,
      "com.typesafe.akka"          %% "akka-stream-testkit"   % AkkaVersion       % Test
    ),
  )
  .enablePlugins(GitVersioning)
  .dependsOn(commons % compileTestScope)

lazy val auctionHousePrimarySync = (project in file("auction-house-primary-sync"))
  .settings(
    commonSettings,
    containerSettings,
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
    containerSettings,
    name := "auction-house-primary-async",
    dockerCommands ++= installBashCommands
  )
  .enablePlugins(JavaAppPackaging, DockerPlugin, GitVersioning)
  .dependsOn(
    baseAsync % compileTestScope,
    commons % compileTestScope
  )

lazy val identityServiceTertiaryAsync = (project in file("identity-service-tertiary-async"))
  .settings(
    commonSettings,
    containerSettings,
    name := "identity-service-tertiary-async",
    dockerCommands ++= installBashCommands
  )
  .enablePlugins(JavaAppPackaging, DockerPlugin, GitVersioning)
  .dependsOn(
    baseAsync % compileTestScope,
    commons % compileTestScope
  )

lazy val identityServiceTertiarySync = (project in file("identity-service-tertiary-sync"))
  .settings(
    commonSettings,
    containerSettings,
    name := "identity-service-tertiary-sync",
    resolvers += Classpaths.typesafeReleases,
    dockerCommands ++= installBashCommands
  )
  .enablePlugins(ScalatraPlugin, JavaAppPackaging, DockerPlugin, GitVersioning)
  .dependsOn(
    commons % compileTestScope,
    baseSync % compileTestScope
  )

lazy val billingServiceSecondarySync = (project in file("billing-service-secondary-sync"))
    .settings(
      commonSettings,
      containerSettings,
      name := "billing-service-secondary-sync",
      resolvers += Classpaths.typesafeReleases,
      dockerCommands ++= installBashCommands,
      libraryDependencies ++= Seq("com.amazonaws" % "aws-java-sdk" % "1.11.306",
        "com.jsuereth" %% "scala-arm" % "2.0")

    )
    .enablePlugins(ScalatraPlugin, JavaAppPackaging, DockerPlugin, GitVersioning)
  .dependsOn(
    commons % compileTestScope,
    baseSync % compileTestScope
  )

lazy val billingServiceSecondaryAsync = (project in file("billing-service-secondary-async"))
  .settings(
    commonSettings,
    containerSettings,
    name := "billing-service-secondary-async",
    dockerCommands ++= installBashCommands,
    libraryDependencies ++= Seq("com.amazonaws" % "aws-java-sdk" % "1.11.306",
      "com.lightbend.akka" %% "akka-stream-alpakka-s3" % "0.18")
  )
  .enablePlugins(JavaAppPackaging, DockerPlugin, GitVersioning)
  .dependsOn(
    baseAsync % compileTestScope,
    commons % compileTestScope
  )

lazy val paymentSystem = (project in file("payment-system"))
  .settings(
    commonSettings,
    containerSettings,
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
    containerSettings,
    name := "gatling-tests",
    libraryDependencies ++= Seq(
      "io.gatling.highcharts"      %  "gatling-charts-highcharts" % "2.3.0",
      "io.gatling"                 %  "gatling-test-framework"    % "2.3.0",
      "org.json4s"                 %% "json4s-jackson"            % "3.6.0-M2",
      "org.json4s"                 %% "json4s-native"             % "3.6.0-M2",
      "com.typesafe"               %  "config"                    % "1.3.2",
      "org.apache.commons"         %  "commons-lang3"             % "3.7",
      "com.amazonaws"              %  "aws-java-sdk-s3"           % "1.11.301",
      "com.github.pathikrit"       %% "better-files"              % "3.4.0"
    ),
    dockerCommands ++= installBashCommands,
    mainClass := Some("com.virtuslab.auctionHouse.perfTests.Runner")
  )
  .enablePlugins(JavaAppPackaging, DockerPlugin, GitVersioning, GatlingPlugin)
  .disablePlugins(CoverallsPlugin)
  .dependsOn(commons % compileTestScope)

lazy val root = (project in file("."))
  .settings(
    commonSettings,
    name := "beyond-the-hype-codebase"
  )
  .aggregate(
    baseSync, baseAsync,
    auctionHousePrimarySync, auctionHousePrimaryAsync,
    billingServiceSecondarySync, billingServiceSecondaryAsync,
    identityServiceTertiarySync, identityServiceTertiaryAsync,
    paymentSystem,
    gatlingTests
  )