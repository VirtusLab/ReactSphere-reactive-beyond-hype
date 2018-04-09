package com.virtuslab.billingsync

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.ObjectMetadata
import com.virtuslab.payments.payments.PaymentRequest
import resource._

import scala.util.Try

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