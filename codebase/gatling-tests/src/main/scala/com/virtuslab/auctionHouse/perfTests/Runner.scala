package com.virtuslab.auctionHouse.perfTests

import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.typesafe.scalalogging.Logger
import com.virtuslab.Logging
import io.gatling.app.Gatling
import io.gatling.core.config.GatlingPropertiesBuilder

import scala.collection.JavaConverters._

object Runner extends App with Logging {

  override protected val log: Logger = Logger(getClass)

  log.info(s"Auction Service URL is: ${Config.auctionServiceContactPoint}")
  log.info(s"Identity Service URL is: ${Config.identityServiceContactPoint}")

  val paradigm = System.getenv().asScala.apply("PARADIGM")

  log.info(s"Waiting (${Config.startDelay}) before starting Gatling tests...")
  Thread.sleep(Config.startDelay.toMillis)

  val props = new GatlingPropertiesBuilder
  props.dataDirectory("jar")
  props.simulationClass(classOf[AuctionHouseSimulation].getName)

  Gatling.fromMap(props.build)

  import better.files._

  val resultsDirectory = File("results")
  println("Result directory generated:")
  resultsDirectory.list.foreach { file =>
    println(s" > ${file.path}")
  }

  if (Config.useS3) {
    println("Upload of reports to S3 started...")

    val resultsZipDirectory = file"/tmp/results"
    resultsZipDirectory.createDirectory()

    resultsDirectory.list.foreach { file =>
      file.zipTo(file"/tmp/results/${file.path.getFileName}.$paradigm.zip")
    }

    println("Prepared all files: ")
    resultsZipDirectory.list.foreach { file =>
      println(s" > ${file.path}")
    }

    val s3Client = s3euWest1

    resultsZipDirectory.list.foreach { file =>
      println(s"Uploading ${file.path}...")
      s3Client.putObject("reactsphere-results", file.name, file.toJava)
    }

    println("Upload of reports to AWS S3 completed!")
  }

  private def s3euWest1 = AmazonS3ClientBuilder.standard().withRegion("eu-west-1").build
}
