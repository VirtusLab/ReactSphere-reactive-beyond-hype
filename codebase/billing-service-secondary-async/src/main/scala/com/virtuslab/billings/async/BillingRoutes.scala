package com.virtuslab.billings.async


import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.ContentTypes.`application/json`
import akka.http.scaladsl.model.HttpMethods.POST
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{HttpEntity, HttpRequest}
import akka.http.scaladsl.server.Directives.{as, complete, entity, handleRejections, onComplete, optionalHeaderValueByName, path, post, _}
import akka.http.scaladsl.server.Route
import com.typesafe.scalalogging.Logger
import com.virtuslab.base.async.{Http, IdentityHelpers, RoutesAuthSupport, RoutingUtils}
import com.virtuslab.payments.payments.{Invoice, PaymentRequest}
import com.virtuslab.{Config, TraceId, TraceIdSupport}
import io.prometheus.client.Histogram
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

import scala.concurrent.ExecutionContext
import scala.concurrent.Future.failed
import scala.util.{Failure, Success}

trait BillingRoutes extends SprayJsonSupport with DefaultJsonProtocol with RoutingUtils with RoutesAuthSupport with S3Service {
  this: IdentityHelpers with TraceIdSupport =>

  implicit lazy val payFormat: RootJsonFormat[PaymentRequest] = jsonFormat3(PaymentRequest)
  implicit lazy val invoiceFormat: RootJsonFormat[Invoice] = jsonFormat1(Invoice)

  protected def logger: Logger

  protected def requestsLatency: Histogram

  protected implicit def executionContext: ExecutionContext

  lazy val httpClient = Http()

  lazy val paymentSystemUrl = s"http://${Config.paymentSystemContactPoint}/api/v1/payment"

  lazy val billingRoutes: Route = handleRejections(rejectionHandler) {
    optionalHeaderValueByName("X-Trace-Id") { maybeTraceId =>
      implicit val traceId: TraceId = extractTraceId(maybeTraceId)
      authenticate(traceId, authenticator) { username =>
        path("billing") {
          post {
            entity(as[PaymentRequest]) { request =>
              logger.info(s"[${traceId.id}] Received payment request '$request'.")
              val histogramTimer = requestsLatency.labels("payForAuction").startTimer()
              val payRequest = HttpRequest(POST, paymentSystemUrl)
                              .withHeaders(RawHeader("X-Trace-Id", traceId.id))
                              .withEntity(HttpEntity(`application/json`, payFormat.write(request).compactPrint))
              onComplete(httpClient.flatMapRequest(payRequest) { httpResponse =>
                if(httpResponse.status.isSuccess()) putInvoice(request) else failed(
                  new RuntimeException("Unexpected response from payment system"))
              }) {
                case Success(invoiceId) =>
                  logger.info(s"[${traceId.id}] payment completed successfully")
                  histogramTimer.observeDuration()
                  complete(OK, Invoice(invoiceId))
                case Failure(exception) =>
                  logger.error(s"[${traceId.id}] Error occurred while requesting payment", exception)
                  histogramTimer.observeDuration()
                  failWith(exception)
              }
            }
          }
        }
      }
    }
  }
}