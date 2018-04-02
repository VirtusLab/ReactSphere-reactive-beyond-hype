package com.virtuslab.billingsync

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.virtuslab.payments.payments.PaymentRequest

import scala.util.{Random, Try}

class S3Service {

  protected lazy val s3 = AmazonS3ClientBuilder.standard()
    .withRegion(Regions.EU_WEST_1)
    .build()


  private val bucket = "reactsphere-billing-service-data"

  def putInvoice(data: PaymentRequest): Try[String] = Try {
    val timestamp = LocalDateTime.now().format(DateTimeFormatter.BASIC_ISO_DATE)
    val objectKey = s"${timestamp}_${data.payee}_${UUID.randomUUID()}"
    s3.putObject(bucket, objectKey,
      s"$timestamp payee: ${data.payee} payer: ${data.payer} amount: ${data.amount}\n" + Random.nextString(10000))
    objectKey
  }
}
