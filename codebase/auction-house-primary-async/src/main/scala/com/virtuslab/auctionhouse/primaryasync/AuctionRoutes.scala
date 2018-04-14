package com.virtuslab.auctionhouse.primaryasync

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.virtuslab.auctions.Categories
import com.virtuslab.base.async.{IdentityHelpers, RoutesAuthSupport, RoutingUtils}
import com.virtuslab.{HeadersSupport, Logging, TraceId, TraceIdSupport}
import io.prometheus.client.Histogram
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

trait AuctionRoutes extends SprayJsonSupport with DefaultJsonProtocol with RoutingUtils with HeadersSupport with RoutesAuthSupport {
  this: AuctionService with IdentityHelpers with TraceIdSupport with Logging =>

  implicit lazy val cauctrFormat: RootJsonFormat[CreateAuctionRequest] = jsonFormat5(CreateAuctionRequest)
  implicit lazy val caFormat: RootJsonFormat[CreatedAuction] = jsonFormat1(CreatedAuction)
  implicit lazy val aiFormat: RootJsonFormat[AuctionInfo] = jsonFormat5(AuctionInfo)
  implicit lazy val bidFormat: RootJsonFormat[Bid] = jsonFormat3(Bid)
  implicit lazy val auctionsFormat: RootJsonFormat[Auctions] = jsonFormat2(Auctions)
  implicit lazy val auctionFormat: RootJsonFormat[AuctionResponse] = jsonFormat9(AuctionResponse)
  implicit lazy val bidReqFormat: RootJsonFormat[BidRequest] = jsonFormat1(BidRequest)

  protected val categoriesSet: Set[String] = Categories.toSet

  protected def requestsLatency: Histogram

  protected implicit def executionContext: ExecutionContext

  lazy val auctionRoutes: Route =
    handleRejections(rejectionHandler) {
      optionalHeaderValueByName("X-Trace-Id") { maybeTraceId =>
        implicit val traceId: TraceId = extractTraceId(maybeTraceId)
        authenticate(traceId, authenticator) { username =>
          path("auctions" / Segment / "bids") { auctionId =>
            post {
              entity(as[BidRequest]) { request =>
                log.info(s"[${traceId.id}] Received bid in auction request for auction '$auctionId'.")

                val histogramTimer = requestsLatency.labels("bidInAuction").startTimer()
                val bid = request.enrich(username, auctionId)

                onComplete(bidInAuction(bid)) {
                  case Success(_) =>
                    log.info(s"[${traceId.id}] Added bid for auction '$auctionId'.")
                    histogramTimer.observeDuration()
                    complete(Created)

                  case Failure(_: BidTooSmall) =>
                    log.warn(s"[${traceId.id}] Bid was too small for auction '$auctionId'.")
                    histogramTimer.observeDuration()
                    complete(Conflict, Error("your bid is not high enough"))

                  case Failure(_: AuctionNotFound) =>
                    log.warn(s"[${traceId.id}] Auction '$auctionId' was not found.")
                    histogramTimer.observeDuration()
                    complete(NotFound)

                  case Failure(exception) =>
                    log.error(s"[${traceId.id}] Error occured while adding bid for auction '$auctionId':", exception)
                    histogramTimer.observeDuration()
                    failWith(exception)
                }
              }
            }
          } ~
            path("finalize" / Segment) { auctionId =>
              post {
                extractRequest { httpRequest =>
                  val token = httpRequest.headers.find(h => AUTHORIZATION_KEYS.contains(h.name()))
                    .flatMap(h => parseAuthHeader(h.value())).get
                  log.info(s"[${traceId.id}] Received pay request for auction '$auctionId'.")

                  val histogramTimer = requestsLatency.labels("auctionFinalization").startTimer()

                  onComplete(payForAuction(auctionId, username, token)) {
                    case Success(_) =>
                      log.info(s"[${traceId.id}] Finalization triggered by '$username'.")
                      histogramTimer.observeDuration()
                      complete(OK)

                    case Failure(_: NotActionWinner) =>
                      log.warn(s"[${traceId.id}] Bidder '$username' is not auction '$auctionId' winner.")
                      histogramTimer.observeDuration()
                      complete(BadRequest, Error("your bid is not high enough"))

                    case Failure(exception) =>
                      log.error(s"[${traceId.id}] Error occurred while paying for auction '$auctionId':", exception)
                      histogramTimer.observeDuration()
                      failWith(exception)
                  }
                }
              }
            } ~
            path("auctions" / Segment) { auctionId =>
              get {
                log.info(s"[${traceId.id}] Got fetch request for auction '$auctionId'.")
                val histogramTimer = requestsLatency.labels("fetchAuction").startTimer()

                onComplete(getAuction(auctionId)) {
                  case Success(auction) =>
                    log.info(s"[${traceId.id}] Fetched auction '$auctionId'.")
                    histogramTimer.observeDuration()
                    complete(OK, auction)

                  case Failure(AuctionNotFound(_)) =>
                    log.warn(s"[${traceId.id}] Auction '$auctionId' was not found!")
                    histogramTimer.observeDuration()
                    complete(NotFound)

                  case Failure(exception) =>
                    log.error(s"[${traceId.id}] Error occured while fetching auction '$auctionId':", exception)
                    histogramTimer.observeDuration()
                    failWith(exception)
                }
              }
            } ~
            path("auctions") {
              get {
                parameter("category") { category =>
                  log.info(s"[${traceId.id}] Got list auctions request for category '$category'.")
                  if (categoriesSet contains category) {
                    val histogramTimer = requestsLatency.labels("listAuctions").startTimer()

                    onComplete(listAuctions(category)) {
                      case Success(listOfAuctions) =>
                        log.info(s"[${traceId.id}] Fetched ${listOfAuctions.size} auctions for category '$category'.")
                        histogramTimer.observeDuration()
                        complete(OK, Auctions(category, listOfAuctions))

                      case Failure(exception) =>
                        log.error(s"[${traceId.id}] Error occured while listing auctions for category '$category':", exception)
                        histogramTimer.observeDuration()
                        failWith(exception)
                    }
                  }
                  else {
                    log.warn(s"[${traceId.id}] Invalid category: '$category'.")
                    complete(BadRequest)
                  }
                }
              } ~
                post {
                  entity(as[CreateAuctionRequest]) { request =>
                    log.info(s"[${traceId.id}] Got create auction request for user '$username'.")
                    val histogramTimer = requestsLatency.labels("createAuction").startTimer()

                    onComplete(createAuction(request addOwner username)) {
                      case Success(auctionId) =>
                        log.info(s"[${traceId.id}] Created auction '$auctionId' for user '$username'.")
                        histogramTimer.observeDuration()
                        complete(Created, CreatedAuction(auctionId))

                      case Failure(exception) =>
                        log.error(
                          s"[${traceId.id}] Error occured while creating auction for user '$username':", exception
                        )
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
