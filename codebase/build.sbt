import com.typesafe.sbt.packager.docker.Cmd
import com.typesafe.sbt.packager.docker.DockerPlugin.autoImport.dockerUpdateLatest

lazy val installBashCommands = Seq(
  Cmd("USER", "root"),
  Cmd("RUN", "apk", "add", "--update", "bash", "&&", "rm", "-rf", "/var/cache/apk/*"),
  Cmd("USER", "daemon")
)

lazy val ScalatraVersion = "2.6.2"
lazy val AkkaHttpVersion = "10.0.11"
lazy val AkkaVersion     = "2.5.8"

lazy val commonSettings = Seq(
  organization        := "com.virtuslab",
  git.baseVersion     := "0.1.0",
  scalaVersion        := "2.12.4",
  dockerBaseImage     := "openjdk:jre-alpine",
  dockerUpdateLatest  := true,
  dockerRepository    := Some(System.getProperty("docker.registry.host", "docker-registry.local")),

  javaOptions in Universal ++= Seq(
    "-XX:+UnlockExperimentalVMOptions",
    "-XX:+UseCGroupMemoryLimitForHeap",
    "-XX:MaxRAMFraction=1",
    "-XshowSettings:vm",
    s"-Dservice.version=${version.value}"
  )
)

lazy val helloWorldSync = (project in file("hello-world-sync"))
  .settings(
    commonSettings,
    name := "hello-world-sync",
    resolvers += Classpaths.typesafeReleases,
    libraryDependencies ++= Seq(
      "org.scalatra"      %% "scalatra"           % ScalatraVersion,
      "org.scalatra"      %% "scalatra-scalatest" % ScalatraVersion   % "test",
      "org.scalatra"      %% "scalatra-json"      % ScalatraVersion,
      "ch.qos.logback"    %  "logback-classic"    % "1.2.3"           % "runtime",
      "org.eclipse.jetty" %  "jetty-webapp"       % "9.4.8.v20171121" % "container;compile",
      "org.eclipse.jetty" %  "jetty-plus"         % "9.4.8.v20171121" % "container;compile",
      "javax.servlet"     %  "javax.servlet-api"  % "3.1.0"           % "provided",
      "org.json4s"        %% "json4s-jackson"     % "3.5.2"
    ),
    dockerCommands ++= installBashCommands
  )
  .enablePlugins(ScalatraPlugin, JavaAppPackaging, DockerPlugin, GitVersioning)

lazy val helloWorldAsync = (project in file("hello-world-async"))
  .settings(
    commonSettings,
    name := "hello-world-async",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http"            % AkkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-spray-json" % AkkaHttpVersion,
      "com.typesafe.akka" %% "akka-stream"          % AkkaVersion,

      "com.typesafe.akka" %% "akka-http-testkit"    % AkkaHttpVersion   % Test,
      "com.typesafe.akka" %% "akka-testkit"         % AkkaVersion       % Test,
      "com.typesafe.akka" %% "akka-stream-testkit"  % AkkaVersion       % Test,
      "org.scalatest"     %% "scalatest"            % "3.0.1"           % Test
    ),
    dockerCommands ++= installBashCommands
  )
  .enablePlugins(JavaAppPackaging, DockerPlugin, GitVersioning)

lazy val root = (project in file("."))
  .settings(
    commonSettings,
    name := "beyond-the-hype-codebase",
  )
  .aggregate(helloWorldSync, helloWorldAsync)