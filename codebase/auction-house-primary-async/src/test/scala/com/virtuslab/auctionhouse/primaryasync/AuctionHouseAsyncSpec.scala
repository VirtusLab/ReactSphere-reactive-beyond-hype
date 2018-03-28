package com.virtuslab.auctionhouse.primaryasync

import java.util.Date

import akka.http.scaladsl.model.ContentTypes.`application/json`
import akka.http.scaladsl.model.HttpMethods.{GET, POST}
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{Authorization, OAuth2BearerToken}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.datastax.driver.core.utils.UUIDs
import com.typesafe.scalalogging.Logger
import com.virtuslab.base.async.TestIdentityHelpers
import com.virtuslab.{RequestMetrics, TraceId, TraceIdSupport}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterEach, GivenWhenThen, Matchers, WordSpec}

import scala.concurrent.ExecutionContext

class AuctionHouseAsyncSpec extends WordSpec with Matchers with ScalaFutures with ScalatestRouteTest
  with GivenWhenThen with BeforeAndAfterEach
  with AuctionRoutes with TestAuctionServiceImpl with TestIdentityHelpers
  with RequestMetrics with TraceIdSupport {

  import com.virtuslab.auctions.Categories
  import spray.json._

  implicit val traceId: TraceId = TraceId(UUIDs.random().toString)

  protected def logger: Logger = Logger(getClass)

  protected def executionContext: ExecutionContext = system.dispatcher

  override def afterEach() = {
    clearAuctionData()
  }

  "Auction module" should {

    val now = (new Date).getTime
    val aBitLater = now + 100L
    val sealedAuctionRoutes = Route.seal(auctionRoutes)

    "reject auction creation requests if authentication token is missing" in {
      When("request to create an auction without authentication token is sent to auction api")
      addValidToken("testuser", "valid-token")
      val body = CreateAuctionRequest(
        category = "test-category",
        title = "test auction",
        description = "some description",
        minimumPrice = BigDecimal(1.23d),
        JsObject()
      ).toJson.prettyPrint
      //      val request = HttpRequest(uri = "/auctions", method = POST)
      //        .withEntity(HttpEntity(`application/json`, body))
      val request = HttpRequest(uri = "/auctions", method = POST)
        .withEntity(HttpEntity(`application/json`, body))

      Then("response should have 401 status and contain an error")
      request ~> sealedAuctionRoutes ~> check {
        status should ===(Unauthorized)
        contentType should ===(`application/json`)
        entityAs[Error] should ===(Error("authentication token is missing"))
      }
    }

    "reject auction creation request if authentication token is invalid" in {
      When("request to create an auction with invalid authentication token is sent to auction api")
      val body = CreateAuctionRequest(
        category = "test-category",
        title = "test auction",
        description = "some description",
        minimumPrice = BigDecimal(1.23d),
        JsObject()
      ).toJson.prettyPrint
      val authHeader = Authorization(OAuth2BearerToken("invalid"))
      val request = HttpRequest(uri = "/auctions", method = POST)
        .withHeadersAndEntity(authHeader :: Nil, HttpEntity(`application/json`, body))

      Then("response should have 403 status and contain an error")
      request ~> sealedAuctionRoutes ~> check {
        status should ===(Forbidden)
        contentType should ===(`application/json`)
        entityAs[Error] should ===(Error("authentication token is invalid"))
      }
    }

    "create auction when submitted with valid authentication token" in {
      Given("valid token for user exists")
      addValidToken("testuser", "valid-token")

      When("request to create an auction with valid authentication token is sent to auction api")
      val expectedAuctionId = getCurrentAuctionId
      val body = CreateAuctionRequest(
        category = "test-category",
        title = "test auction",
        description = "some description",
        minimumPrice = BigDecimal(1.23d),
        JsObject()
      ).toJson.prettyPrint
      val authHeader = Authorization(OAuth2BearerToken("valid-token"))
      val request = HttpRequest(uri = "/auctions", method = POST)
        .withHeadersAndEntity(authHeader :: Nil, HttpEntity(`application/json`, body))

      Then("response should have 201 status and contain an auction id")
      request ~> sealedAuctionRoutes ~> check {
        status should ===(Created)
        contentType should ===(`application/json`)
        entityAs[CreatedAuction] should ===(CreatedAuction(expectedAuctionId))
      }
    }

    "reject authenticated request for auctions list without category" in {
      Given("valid token for user exists")
      addValidToken("testuser", "valid-token")

      When("request to list auctions with valid authentication token but without category parameter is sent to auction api")
      val authHeader = Authorization(OAuth2BearerToken("valid-token"))
      val request = HttpRequest(uri = "/auctions", method = GET)
        .withHeaders(authHeader :: Nil)

      Then("response should have 400 status")
      request ~> sealedAuctionRoutes ~> check {
        status should ===(BadRequest)
      }
    }

    "list existing auctions for given category when requested with correct authentication token" in {
      Given("two auctions exist in auction api")
      addAuction(Categories(1), "au1", "testuser2", "auction 1", now)
      addAuction(Categories(1), "au2", "testuser3", "auction 2", aBitLater)

      And("valid token for user exists")
      addValidToken("testuser", "valid-token")

      When("request to list auctions with valid authentication token is sent to auction api")
      val authHeader = Authorization(OAuth2BearerToken("valid-token"))
      val request = HttpRequest(uri = s"/auctions?category=${Categories(1)}", method = GET)
        .withHeaders(authHeader :: Nil)

      Then("response should have 200 status and contain a list of auctions")
      request ~> sealedAuctionRoutes ~> check {
        status should ===(OK)
        contentType should ===(`application/json`)
        entityAs[Auctions] should ===(Auctions(
          category = Categories(1),
          List(
            AuctionInfo("au1", now, "testuser2", "auction 1", BigDecimal(0)),
            AuctionInfo("au2", aBitLater, "testuser3", "auction 2", BigDecimal(0))
          )
        ))
      }
    }

    "fetch existing auction when requested by id with correct authentication token" in {
      Given("two auctions exist in auction api")
      addAuction("test-category", "au1", "testuser2", "auction 1", now)
      addAuction("test-category", "au2", "testuser3", "auction 2", aBitLater)

      And("valid token for user exists")
      addValidToken("testuser", "valid-token")

      When("request to fetch auction with id au2 and with valid authentication token is sent to auction api")
      val authHeader = Authorization(OAuth2BearerToken("valid-token"))
      val request = HttpRequest(uri = "/auctions/au2", method = GET, headers = authHeader :: Nil)

      Then("response should have 200 status and contain auction information")
      request ~> sealedAuctionRoutes ~> check {
        status should ===(OK)
        contentType should ===(`application/json`)
        entityAs[AuctionResponse] should ===(AuctionResponse(
          category = "test-category",
          auctionId = "au2",
          createdAt = aBitLater,
          owner = "testuser3",
          title = "auction 2",
          description = "",
          minimumPrice = BigDecimal(0),
          details = JsObject(),
          bids = List.empty
        ))
      }
    }

    "bid in auction that doesn't exist with correct authentication token" in {
      Given("valid token for user exists")
      addValidToken("testuser", "valid-token")

      When("request to bid in non-existing auction with with valid authentication token is sent to auction api")
      val authHeader = Authorization(OAuth2BearerToken("valid-token"))
      val body = BidRequest(BigDecimal(10.0d)).toJson.prettyPrint
      val createBidRequest = HttpRequest(uri = "/auctions/au1/bids", method = POST)
        .withHeadersAndEntity(authHeader :: Nil, HttpEntity(`application/json`, body))

      Then("response should have 404 status")
      createBidRequest ~> sealedAuctionRoutes ~> check {
        status should ===(NotFound)
      }
    }

    "bid in existing auction without any previous bids with correct authentication token" in {
      Given("an auction exists in auction api")
      addAuction("test-category", "au1", "testuser2", "auction 1", now)
      val expectedBidId = getCurrentBidId

      And("valid token for user exists")
      addValidToken("testuser", "valid-token")

      When("request to bid in auction with with valid authentication token is sent to auction api")
      val authHeader = Authorization(OAuth2BearerToken("valid-token"))
      val body = BidRequest(BigDecimal(10.0d)).toJson.prettyPrint
      val createBidRequest = HttpRequest(uri = "/auctions/au1/bids", method = POST)
        .withHeadersAndEntity(authHeader :: Nil, HttpEntity(`application/json`, body))

      Then("response should have 201 status")
      createBidRequest ~> sealedAuctionRoutes ~> check {
        status should ===(Created)
      }

      When("request to fetch auction by id with valid authentication token is sent to auction api")
      val fetchAuctionRequest = HttpRequest(uri = "/auctions/au1", method = GET, headers = authHeader :: Nil)

      Then("response should have 200 status and contain created bid")
      fetchAuctionRequest ~> sealedAuctionRoutes ~> check {
        status should ===(OK)
        contentType should ===(`application/json`)
        entityAs[AuctionResponse] should ===(
          AuctionResponse(
            category = "test-category",
            auctionId = "au1",
            createdAt = now,
            owner = "testuser2",
            title = "auction 1",
            description = "",
            minimumPrice = BigDecimal(0),
            details = JsObject(),
            bids = List(Bid(expectedBidId, "testuser", BigDecimal(10)))
          ))
      }
    }

    "bid in existing auction with too small bid and with correct authentication token" in {
      Given("an auction exists in auction api and a bid with amount 15 for that auction exists")
      addAuctionWithBids("test-category", "au1", "testuser2", "auction 1",
        Bid("some-bid", "anotheruser", BigDecimal(15d)) :: Nil, now)

      And("valid token for user exists")
      addValidToken("testuser", "valid-token")

      When("request to bid in auction with amount 10 is sent to auction api with valid authentication token")
      val authHeader = Authorization(OAuth2BearerToken("valid-token"))
      val body = BidRequest(BigDecimal(10.0d)).toJson.prettyPrint
      val createBidRequest = HttpRequest(uri = "/auctions/au1/bids", method = POST)
        .withHeadersAndEntity(authHeader :: Nil, HttpEntity(`application/json`, body))

      Then("response should have 409 status")
      createBidRequest ~> sealedAuctionRoutes ~> check {
        status should ===(Conflict)
        contentType should ===(`application/json`)
        entityAs[Error] should ===(Error("your bid is not high enough"))
      }
    }


  }

}
