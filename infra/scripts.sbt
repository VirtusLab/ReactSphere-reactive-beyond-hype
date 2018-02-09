lazy val scripts = (project in file("."))
  .settings(libraryDependencies ++= Seq(
    "com.lihaoyi" %% "ammonite-ops" % "1.0.3"
  ))