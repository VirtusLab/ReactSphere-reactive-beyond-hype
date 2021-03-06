package com.virtuslab.billingsync

import com.typesafe.scalalogging.Logger
import com.virtuslab.base.sync.Http
import com.virtuslab.billingsync.BillingService.TransactionId
import com.virtuslab.payments.payments.PaymentRequest
import com.virtuslab.{Config, HeadersSupport, Logging, TraceId}
import org.json4s.jackson.Serialization.write
import org.json4s.{DefaultFormats, Formats}

import scala.util.{Failure, Random, Try}

object BillingService {
  case class UserId(login: String)

  case class TransactionId(id: String)
  object TransactionId {
    private val random = new Random()

    def fresh: TransactionId = {
      TransactionId(random.alphanumeric.take(10).mkString)
    }
  }
}

class BillingService extends HeadersSupport with Logging {

  protected implicit val jsonFormats: Formats = DefaultFormats

  override val log = Logger(getClass.toString)

  private val paymentSystemUrl = s"http://${Config.paymentSystemContactPoint}/api/v1/payment"
  log.info(s"Payment system url is: ${paymentSystemUrl}")

  def performPayment(paymentRequest: PaymentRequest)(implicit traceId: TraceId): Try[TransactionId] = {
    val body = write(paymentRequest)
    val response = Http(paymentSystemUrl)
      .headers(traceHeaders)
      .postData(body)
      .asString

    if (response.code == 200) {
      Try(TransactionId.fresh)
    } else {
      Failure(new Exception(s"Transaction payment failed with code: ${response.code}"))
    }
  }
}
