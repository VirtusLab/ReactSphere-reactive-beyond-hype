package com.virtuslab.auctionHouse.sync.finalization

import com.typesafe.scalalogging.Logger
import com.virtuslab._
import com.virtuslab.billing.BillingRequest
import org.json4s.{DefaultFormats, Formats}
import org.json4s.jackson.Serialization.write

import scalaj.http.Http

class FinalizationService extends TraceIdSupport with Logging with HeadersSupport {

  override val log = Logger(getClass.toString)

  private implicit val jsonFormats: Formats = DefaultFormats

  private val billingUrl = s"http://${Config.billingServiceConctactPoint}/api/v1/billing"
  log.info(s"Billing url is: ${billingUrl}")

  def finalizeAuction()(implicit traceId: TraceId, authToken: Option[AuthToken]): Unit = {
    val body = write(BillingRequest("user1", "user2", 5000))
    val response = Http(billingUrl)
      .headers(traceHeaders ++ authHeaders)
      .postData(body)
      .asString

    if(response.is2xx) {
      log.info(s"Success payment fulfilled for user: xxxx")
    } else {
      log.error(s"Billing request failed. Code: ${response.code}, msg: ${response.body}")
    }
  }
}
