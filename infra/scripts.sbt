lazy val scripts = (project in file("."))
  .settings(libraryDependencies ++= Seq(
    "com.lihaoyi" %% "ammonite-ops" % "1.0.3",
    "com.amazonaws" % "aws-java-sdk-route53" % "1.11.307",
    "com.amazonaws" % "aws-java-sdk-elasticloadbalancing" % "1.11.307"
  ))