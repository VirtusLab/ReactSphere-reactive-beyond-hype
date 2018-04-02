package com.virtuslab.billings.async

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.alpakka.s3.S3Settings
import akka.stream.alpakka.s3.scaladsl.{MultipartUploadResult, S3Client}
import akka.stream.scaladsl.{Sink, Source}
import akka.util.ByteString
import com.virtuslab.payments.payments.PaymentRequest

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Random

trait S3Service {

  protected implicit def system: ActorSystem
  protected implicit val materializer: Materializer

  private val bucket = "reactsphere-billing-service-data"
  protected lazy val s3Client = new S3Client(S3Settings())

  def putInvoice(data: PaymentRequest): Future[String] = {
    val timestamp = LocalDateTime.now().format(DateTimeFormatter.BASIC_ISO_DATE)
    val content = s"$timestamp payee: ${data.payee} payer: ${data.payer} amount: ${data.amount}\n" + Random.nextString(10000)
    val objectKey = s"${timestamp}_${data.payee}_${UUID.randomUUID()}"
    val s3Sink: Sink[ByteString, Future[MultipartUploadResult]] = s3Client.multipartUpload(bucket, objectKey)
    val src = Source.single[ByteString](ByteString(content))
    val res = src.runWith(s3Sink)
    res.map(_ => objectKey)
  }
}
