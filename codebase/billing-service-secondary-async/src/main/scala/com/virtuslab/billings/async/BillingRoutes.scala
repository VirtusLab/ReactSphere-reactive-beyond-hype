package com.virtuslab.billings.async

import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.ContentTypes.`application/json`
import akka.http.scaladsl.model.HttpMethods.POST
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{HttpEntity, HttpRequest}
import akka.http.scaladsl.server.Directives.{as, complete, entity, handleRejections, onComplete, optionalHeaderValueByName, path, post, _}
import akka.http.scaladsl.server.Route
import com.typesafe.scalalogging.Logger
import com.virtuslab.base.async.{IdentityHelpers, RoutesAuthSupport, RoutingUtils}
import com.virtuslab.{Config, TraceId, TraceIdSupport}
import io.prometheus.client.Histogram
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

trait BillingRoutes extends SprayJsonSupport with DefaultJsonProtocol with RoutingUtils with RoutesAuthSupport {
  this: IdentityHelpers with TraceIdSupport =>

  implicit lazy val payFormat: RootJsonFormat[PayRequest] = jsonFormat3(PayRequest)

  protected def logger: Logger

  protected def requestsLatency: Histogram

  protected implicit def executionContext: ExecutionContext

  lazy val paymentSystemUrl = s"http://${Config.paymentSystemContactPoint}/api/v1/payment"

  lazy val billingRoutes: Route = handleRejections(rejectionHandler) {
    optionalHeaderValueByName("X-Trace-Id") { maybeTraceId =>
      implicit val traceId: TraceId = extractTraceId(maybeTraceId)
      authenticate(traceId, authenticator) { username =>
        path("billing") {
          post {
            entity(as[PayRequest]) { request =>
              logger.info(s"[${traceId.id}] Received payment request '$request'.")
              val payRequest = HttpRequest(POST, paymentSystemUrl)
                              .withHeaders(RawHeader("X-Trace-Id", traceId.id))
                              .withEntity(HttpEntity(`application/json`, request.toJson.compactPrint))

              onComplete(Http().singleRequest(payRequest)) {
                case Success(response) => {
                  if (response.status.isSuccess())
                    complete(OK)
                  else {
                    logger.error(s"Unexpected response: $response")
                    complete(ServiceUnavailable, Error("Unexpected response from payment system"))
                  }
                }
                case Failure(exception) =>
                  logger.error(s"[${traceId.id}] Error occurred while requesting payment", exception)
                  failWith(exception)
              }

            }
          }
        }
      }
    }
  }

  case class PayRequest(payer: String, payee: String, amount: BigDecimal)

}