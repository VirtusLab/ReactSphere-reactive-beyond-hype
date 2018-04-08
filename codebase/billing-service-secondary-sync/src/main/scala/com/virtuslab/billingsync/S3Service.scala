package com.virtuslab.billingsync

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.ObjectMetadata
import com.typesafe.scalalogging.Logger
import com.virtuslab.Logging
import com.virtuslab.payments.payments.PaymentRequest
import resource._

import scala.util.Try

object S3Service extends Logging {
  override protected val log: Logger = Logger(getClass)

  def logAwsKeys(obfuscate: Boolean = true): Unit = {
    val key = Option(System.getenv("AWS_ACCESS_KEY_ID"))
    val secret = Option(System.getenv("AWS_SECRET_ACCESS_KEY"))

    val awsVals = Seq(key, secret).map {
      case Some(value) =>
        if(obfuscate) {
          val prefixLength = 5
          val suffixLength = value.length - prefixLength
          value.substring(0, prefixLength) + 0.until(suffixLength).map(_ => "*").mkString
        } else {
          value
        }

      case None =>
        "<NOT SET>"
    }

    log.info(s"Read AWS credentials are: ${awsVals.head} / ${awsVals.tail.head}")
  }
}

class S3Service {

  protected lazy val s3 = AmazonS3ClientBuilder.standard()
    .withRegion(Regions.EU_WEST_1)
    .build()


  private val bucket = "reactsphere-billing-service-data"

  def putInvoice(data: PaymentRequest): Try[String] = Try {
    val timestamp = LocalDateTime.now().format(DateTimeFormatter.BASIC_ISO_DATE)
    val objectKey = s"${timestamp}_${data.payee}_${UUID.randomUUID()}"
    for(inputStream <- managed(getClass.getResourceAsStream("/dummy_invoice.pdf"))) {
      s3.putObject(bucket, objectKey, inputStream, new ObjectMetadata())
    }
    objectKey
  }
}