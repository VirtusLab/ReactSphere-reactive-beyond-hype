package com.virtuslab.auctionhouse.primaryasync

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.AuthenticationFailedRejection._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route.seal
import akka.http.scaladsl.server.directives.Credentials
import akka.http.scaladsl.server.{RejectionHandler, Route, _}
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

import scala.concurrent.Future
import scala.util.{Failure, Success}

trait AuctionRoutes extends SprayJsonSupport with DefaultJsonProtocol with RoutingUtils {
  this: AuctionService with IdentityHelpers =>

  implicit lazy val cauctrFormat: RootJsonFormat[CreateAuctionRequest] = jsonFormat5(CreateAuctionRequest)
  implicit lazy val mteFormat: RootJsonFormat[MissingTokenError] = jsonFormat1(MissingTokenError)
  implicit lazy val iteFormat: RootJsonFormat[InvalidTokenError] = jsonFormat1(InvalidTokenError)
  implicit lazy val caFormat: RootJsonFormat[CreatedAuction] = jsonFormat1(CreatedAuction)
  implicit lazy val aiFormat: RootJsonFormat[AuctionInfo] = jsonFormat5(AuctionInfo)
  implicit lazy val bidFormat: RootJsonFormat[Bid] = jsonFormat3(Bid)
  implicit lazy val auctionsFormat: RootJsonFormat[Auctions] = jsonFormat2(Auctions)
  implicit lazy val auctionFormat: RootJsonFormat[AuctionResponse] = jsonFormat9(AuctionResponse)
  implicit lazy val bidReqFormat: RootJsonFormat[BidRequest] = jsonFormat1(BidRequest)

  implicit def rejectionHandler: RejectionHandler =
    RejectionHandler.newBuilder()
      .handle { case MissingQueryParamRejection(_) =>
        complete(BadRequest)
      }
      .handle { case AuthenticationFailedRejection(cause, _) =>
        cause match {
          case CredentialsMissing => complete((Unauthorized, MissingTokenError()))
          case CredentialsRejected => complete((Forbidden, InvalidTokenError()))
        }
      }
      .result()

  lazy val authenticator: Credentials => Future[Option[String]] = {
    case Credentials.Provided(token) => validateToken(token)
    case _ => Future.successful(None)
  }
  lazy val auctionRoutes: Route =
    extractSettings { implicit settings =>
      seal {
        authenticate { username =>
          pathPrefix("api") {
            pathPrefix("v1") {
              path("auctions" / Segment / "bids") { auctionId =>
                post {
                  entity(as[BidRequest]) { request =>
                    val bid = request.enrich(username, auctionId)
                    onComplete(bidInAuction(bid)) {
                      case Success(_) => complete(Created)
                      case Failure(_: BidTooSmall) => complete(Conflict, Error("your bid is not high enough"))
                      case Failure(_: AuctionNotFound) => complete(NotFound)
                      case Failure(exception) => failWith(exception)
                    }
                  }
                }
              } ~
                path("auctions" / Segment) { auctionId =>
                  onComplete(getAuction(auctionId)) {
                    case Success(auction) => complete(OK, auction)
                    case Failure(exception) => failWith(exception)
                  }
                } ~
                path("auctions") {
                  get {
                    parameter("category") { category =>
                      onComplete(listAuctions(category)) {
                        case Success(listOfAuctions) => complete(OK, Auctions(category, listOfAuctions))
                        case Failure(exception) => failWith(exception)
                      }
                    }
                  } ~
                    post {
                      entity(as[CreateAuctionRequest]) { request =>
                        onComplete(createAuction(request addOwner username)) {
                          case Success(auctionId) => complete(Created, CreatedAuction(auctionId))
                          case Failure(exception) => failWith(exception)
                        }
                      }
                    }
                }
            }
          }
        }
      }
    }

  def authenticate: Directive1[String] = authenticateOAuth2Async(realm = "auction-house", authenticator)

}
