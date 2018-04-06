package com.virtuslab.auctionHouse.perfTests

import com.virtuslab.auctionHouse.perfTests.AuctionsActions._
import io.gatling.core.Predef._
import io.gatling.core.session.Expression
import io.gatling.http.Predef._
import org.json4s.JValue
import org.json4s.jackson.JsonMethods.parse
import org.json4s.jackson.Serialization.write

class AuctionsActions(errorHandler: ErrorHandler) extends BaseActions(errorHandler) {

  def url(path: String) =  s"http://${Config.auctionServiceContactPoint}/api/${Config.apiVersion}/$path"

  protected val getAuctionUrl: Expression[String] = (session: Session) => {
    session(auctionsParam).asOption[Auctions]
      .map {
        auctions =>
          if (auctions.auctions.isEmpty) {
            errorHandler.raiseError("Empty auction list in session")
          }
          val auctionId = randSeqValue(auctions.auctions).auctionId
          url(s"auctions/$auctionId")
      }
      .getOrElse(errorHandler.raiseError("Auction list not found in gatling session"))
  }

  protected val bidInAuctionUrl: Expression[String] = (session: Session) => {
    session(selectedAuctionParam).asOption[AuctionViewResponse]
      .map(a => url(s"auctions/${a.auctionId}/bids"))
      .getOrElse(errorHandler.raiseError("Auction not found in gatling session"))
  }

  protected val bidInAuctionBody: Expression[String] = (session: Session) => {
    session(selectedAuctionParam).asOption[AuctionViewResponse]
      .map { a =>
        val nextBid = a.bids.map(_.amount).reduceOption(_ max _)
          .map(_ + randPosNum)
          .getOrElse(randPosNum)
        write(BidRequest(nextBid))
      }.getOrElse(errorHandler.raiseError("Auction not found in gatling session"))
  }

  protected val payForAuctionUrl: Expression[String] = (session: Session) => {
    session(selectedAuctionParam).asOption[AuctionViewResponse]
      .map(a => url(s"finalize/${a.auctionId}"))
      .getOrElse(errorHandler.raiseError("Auction not found in gatling session"))
  }

  def randAuction(category: String) = CreateAuctionRequest(category, randStr, randStr, randPosNum,
    parse(s"""{"$randStr": "$randStr"}"""))


  def createAuction(category: String) = {
    http("create auction")
      .post(url("auctions"))
      .withAuthHeaders()
      .body(StringBody(write(randAuction(category))))
      .check(
        status.is(201)
      )
  }

  def listAuctions(category: String) = {
    http("list auctions")
      .get(url("auctions"))
      .withAuthHeaders()
      .queryParam("category", category)
      .check(
        status.is(200),
        bodyString.transform(parse(_).extract[Auctions]).saveAs(auctionsParam)
      )
  }

  def getAuction = {
    http("get auction")
      .get(getAuctionUrl)
      .withAuthHeaders()
      .check(
        status.is(200),
        bodyString.transform(parse(_).extract[AuctionViewResponse]).saveAs(selectedAuctionParam)
      )
  }

  def bidInAuction = {
    http("Bid in auction")
      .post(bidInAuctionUrl)
      .withAuthHeaders()
      .body(StringBody(bidInAuctionBody))
      .check(
        status.in(201, 409)
      )
  }

  def getAuctionWithBids = {
    val getAuctionBuilder = getAuction
    getAuctionBuilder
      .copy(commonAttributes = getAuctionBuilder.commonAttributes.copy(requestName = "get auction with bids"))
      .check(
        bodyString.transform(parse(_).extract[AuctionViewResponse].bids.size).greaterThanOrEqual(1)
      )
  }

  def payForAuction = {
    http("Pay for auction")
      .post(payForAuctionUrl)
      .withAuthHeaders()
      .check(
        status.in(200)
      )
  }
}

object AuctionsActions {

  val auctionsParam = "auctions"
  val selectedAuctionParam = "selectedAuction"

  case class CreateAuctionRequest(category: String, title: String, description: String, minimumPrice: BigDecimal,
                                  details: JValue)

  case class AuctionInfo(auctionId: String, createdAt: Long, owner: String, title: String, minimumPrice: BigDecimal)

  case class Auctions(category: String, auctions: Seq[AuctionInfo])

  case class Bid(bidId: String, bidder: String, amount: BigDecimal)

  case class AuctionViewResponse(auctionId: String, owner: String, title: String, description: String, details: JValue,
                                 bids: Seq[Bid])

  case class BidRequest(amount: BigDecimal)

}