package com.virtuslab.billings.async

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.alpakka.s3.S3Settings
import akka.stream.alpakka.s3.scaladsl.{MultipartUploadResult, S3Client}
import akka.stream.scaladsl.{Sink, StreamConverters}
import akka.util.ByteString
import com.typesafe.scalalogging.Logger
import com.virtuslab.Logging
import com.virtuslab.payments.payments.PaymentRequest

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait S3Service extends Logging {

  override val log = Logger(getClass.toString)

  protected implicit def system: ActorSystem
  protected implicit val materializer: Materializer

  private val bucket = "reactsphere-billing-service-data"
  protected lazy val s3Client = new S3Client(S3Settings())

  def putInvoice(data: PaymentRequest): Future[String] = {
    val timestamp = LocalDateTime.now().format(DateTimeFormatter.BASIC_ISO_DATE)
    val objectKey = s"${timestamp}_${data.payee}_${UUID.randomUUID()}"
    val s3Sink: Sink[ByteString, Future[MultipartUploadResult]] = s3Client.multipartUpload(bucket, objectKey)
    StreamConverters
      .fromInputStream(() => getClass.getResourceAsStream("/dummy_invoice.pdf"))
      .runWith(s3Sink).map(_ => objectKey)
  }
}
