package com.virtuslab.auctionHouse.perfTests

import java.io.File

import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.transfer.TransferManagerBuilder
import io.gatling.app.Gatling
import io.gatling.core.config.GatlingPropertiesBuilder

object Runner extends App {

  println(s"Waiting (${Config.startDelay}) before starting Gatling tests...")
  Thread.sleep(Config.startDelay.toMillis)

  val props = new GatlingPropertiesBuilder
  props.dataDirectory("jar")
  props.simulationClass(classOf[AuctionHouseSimulation].getName)

  Gatling.fromMap(props.build)

  val resultsDirectory = new File("results")
  println("Result directory generated:")
  resultsDirectory.listFiles.foreach { file =>
    println(s" > ${file.getAbsolutePath}")
  }

  println()

  if (Config.useS3) {
    val transferManagerBuilder = TransferManagerBuilder.standard()
    transferManagerBuilder.setS3Client(s3euWest1)

    val transferManager = transferManagerBuilder.build()
    val transfer = transferManager.uploadDirectory("reactsphere-results", "", resultsDirectory, true)

    transfer.waitForCompletion()
    transferManager.shutdownNow()

    println("Upload of reports to AWS S3 completed!")
  }

  private def s3euWest1 = AmazonS3ClientBuilder.standard().withRegion("eu-west-1").build

}
