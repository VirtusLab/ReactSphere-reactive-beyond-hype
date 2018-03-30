package com.virtuslab.billingsync

import com.virtuslab.billingsync.BillingService.{TransactionId, UserId}
import com.virtuslab.payments.PaymentModel.PaymentRequest
import com.virtuslab.{Config, TraceId, TraceIdSupport}
import org.json4s.{DefaultFormats, Formats}
import org.json4s.jackson.Serialization.write

import scala.util.{Failure, Random, Try}
import scalaj.http.Http

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

class BillingService extends TraceIdSupport {

  protected implicit val jsonFormats: Formats = DefaultFormats

  private lazy val paymentSystemUrl = s"http://${Config.paymentSystemContactPoint}/api/v1/payment"

  def performPayment(payer: UserId, payee: UserId)(implicit traceId: TraceId): Try[TransactionId] = {
    val body = write(PaymentRequest("me", "you", 5000))
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
