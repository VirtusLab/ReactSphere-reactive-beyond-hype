package com.virtuslab.auctionhouse.primaryasync

import akka.http.scaladsl.model.ContentTypes.`application/json`
import akka.http.scaladsl.model.HttpMethods.{GET, POST}
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{Authorization, OAuth2BearerToken}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.virtuslab.identity.{CreateAccountRequest, SignInRequest}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterEach, GivenWhenThen, Matchers, WordSpec}

class AuctionHouseAsyncSpec extends WordSpec with Matchers with ScalaFutures with ScalatestRouteTest
  with GivenWhenThen with BeforeAndAfterEach
  with IdentityRoutes with TestIdentityServiceImpl
  with AuctionRoutes with TestAuctionServiceImpl with IdentityHelpers {

  import spray.json._

  override def afterEach() = {
    clearData()
    clearAuctionData()
  }

  "Identity module" should {
    "create accounts" in {
      When("request to create account is sent to identity api")
      val body = CreateAccountRequest("testuser", "testpassword").toJson.prettyPrint
      val request = HttpRequest(uri = "/api/v1/accounts", method = POST)
        .withEntity(HttpEntity(`application/json`, body))

      Then("response should have 201 status")
      request ~> identityRoutes ~> check {
        status should ===(Created)
      }
    }

    "return error on duplicate account creation" in {
      Given("user testuser exists")
      addUser("testuser", "testpassword")

      When("request to create account with the same username is sent to identity api")
      val body = CreateAccountRequest("testuser", "testpassword").toJson.prettyPrint
      val request = HttpRequest(uri = "/api/v1/accounts", method = POST)
        .withEntity(HttpEntity(`application/json`, body))

      Then("response should have 400 status and contain an error")
      request ~> identityRoutes ~> check {
        status should ===(BadRequest)
        contentType should ===(`application/json`)
        entityAs[String] should ===("""{"error":"user testuser already exists"}""")
      }
    }

    "sign in existing users" in {
      Given("user testuser exists and has password testpassword")
      addUser("testuser", "testpassword")

      When("request to sign in existing user with correct password is sent to identity api")
      val body = SignInRequest("testuser", "testpassword").toJson.prettyPrint
      val request = HttpRequest(uri = "/api/v1/sign-in", method = POST)
        .withEntity(HttpEntity(`application/json`, body))

      Then("response should have 200 status containing token")
      request ~> identityRoutes ~> check {
        status should ===(OK)
        contentType should ===(`application/json`)
        entityAs[String] should ===("""{"token":"f60e7614-0d56-4ef5-b6c8-3939d34d5ec2"}""")
      }
    }

    "reject sign in attempts when password is incorrect" in {
      Given("user testuser exists and has password testpassword")
      addUser("testuser", "testpassword")

      When("request to sign in existing user with incorrect password is sent to identity api")
      val invalidPasswordBody = SignInRequest("testuser", "invalid").toJson.prettyPrint
      val invalidPasswordRequest = HttpRequest(uri = "/api/v1/sign-in", method = POST)
        .withEntity(HttpEntity(`application/json`, invalidPasswordBody))

      Then("response should have 401 status and contain an error")
      invalidPasswordRequest ~> identityRoutes ~> check {
        status should ===(Unauthorized)
        contentType should ===(`application/json`)
        entityAs[String] should ===("""{"error":"wrong password or username"}""")
      }
    }

  }

  "Auction module" should {

    "reject auction creation requests if authentication token is missing" in {
      When("request to create an auction without authentication token is sent to auction api")
      val body = CreateAuctionRequest(
        title = "test auction",
        description = "some description",
        minimumPrice = BigDecimal(1.23d),
        JsObject()
      ).toJson.prettyPrint
      val request = HttpRequest(uri = "/api/v1/auctions", method = POST)
        .withEntity(HttpEntity(`application/json`, body))

      Then("response should have 401 status and contain an error")
      request ~> auctionRoutes ~> check {
        status should ===(Unauthorized)
        contentType should ===(`application/json`)
        entityAs[String] should ===("""{"error":"authentication token is missing"}""")
      }
    }

    "reject auction creation request if authentication token is invalid" in {
      When("request to create an auction with invalid authentication token is sent to auction api")
      val body = CreateAuctionRequest(
        title = "test auction",
        description = "some description",
        minimumPrice = BigDecimal(1.23d),
        JsObject()
      ).toJson.prettyPrint
      val authHeader = Authorization(OAuth2BearerToken("invalid"))
      val request = HttpRequest(uri = "/api/v1/auctions", method = POST)
        .withHeadersAndEntity(authHeader :: Nil, HttpEntity(`application/json`, body))

      Then("response should have 403 status and contain an error")
      request ~> auctionRoutes ~> check {
        status should ===(Forbidden)
        contentType should ===(`application/json`)
        entityAs[String] should ===("""{"error":"authentication token is invalid"}""")
      }
    }

    "create auction when submitted with valid authentication token" in {
      When("request to create an auction with valid authentication token is sent to auction api")
      val expectedAuctionId = getCurrentAuctionId
      val body = CreateAuctionRequest(
        title = "test auction",
        description = "some description",
        minimumPrice = BigDecimal(1.23d),
        JsObject()
      ).toJson.prettyPrint
      val authHeader = Authorization(OAuth2BearerToken("valid-token"))
      val request = HttpRequest(uri = "/api/v1/auctions", method = POST)
        .withHeadersAndEntity(authHeader :: Nil, HttpEntity(`application/json`, body))

      Then("response should have 201 status and contain an auction id")
      request ~> auctionRoutes ~> check {
        status should ===(Created)
        contentType should ===(`application/json`)
        entityAs[String] should ===(s"""{"auctionId":"$expectedAuctionId"}""")
      }
    }

    "list existing auctions when requested with correct authentication token" in {
      Given("two auctions exist in auction api")
      addAuction("au1", "testuser2", "auction 1")
      addAuction("au2", "testuser3", "auction 2")

      When("request to list auctions with valid authentication token is sent to auction api")
      val authHeader = Authorization(OAuth2BearerToken("valid-token"))
      val request = HttpRequest(uri = "/api/v1/auctions", method = GET)
        .withHeaders(authHeader :: Nil)

      Then("response should have 200 status and contain a list of auctions")
      request ~> auctionRoutes ~> check {
        status should ===(OK)
        contentType should ===(`application/json`)
        entityAs[String] should ===(
          s"""{"auctions":[
             |{"auctionId":"au1","owner":"testuser2","title":"auction 1","minimumPrice":0.0},
             |{"auctionId":"au2","owner":"testuser3","title":"auction 2","minimumPrice":0.0}
             |]}""".stripMargin.filter(_ >= ' ')
        )
      }
    }

    "fetch existing auction when requested by id with correct authentication token" in {
      Given("two auctions exist in auction api")
      addAuction("au1", "testuser2", "auction 1")
      addAuction("au2", "testuser3", "auction 2")

      When("request to fetch auction with id au2 and with valid authentication token is sent to auction api")
      val authHeader = Authorization(OAuth2BearerToken("valid-token"))
      val request = HttpRequest(uri = "/api/v1/auctions/au2", method = GET, headers = authHeader :: Nil)

      Then("response should have 200 status and contain auction information")
      request ~> auctionRoutes ~> check {
        status should ===(OK)
        contentType should ===(`application/json`)
        entityAs[String] should ===(
          s"""{
             |"description":"",
             |"bids":[],
             |"auctionId":"au2",
             |"minimumPrice":0.0,
             |"details":{},
             |"owner":"testuser3",
             |"title":"auction 2"
             |}""".stripMargin.filter(_ >= ' ')
        )
      }
    }

    "bid in auction that doesn't exist with correct authentication token" in {
      When("request to bid in non-existing auction with with valid authentication token is sent to auction api")
      val authHeader = Authorization(OAuth2BearerToken("valid-token"))
      val body = BidRequest(BigDecimal(10.0d)).toJson.prettyPrint
      val createBidRequest = HttpRequest(uri = "/api/v1/auctions/au1/bids", method = POST)
        .withHeadersAndEntity(authHeader :: Nil, HttpEntity(`application/json`, body))

      Then("response should have 404 status")
      createBidRequest ~> auctionRoutes ~> check {
        status should ===(NotFound)
      }
    }

    "bid in existing auction without any previous bids with correct authentication token" in {
      Given("an auction exists in auction api")
      addAuction("au1", "testuser2", "auction 1")
      val expectedBidId = getCurrentBidId

      When("request to bid in auction with with valid authentication token is sent to auction api")
      val authHeader = Authorization(OAuth2BearerToken("valid-token"))
      val body = BidRequest(BigDecimal(10.0d)).toJson.prettyPrint
      val createBidRequest = HttpRequest(uri = "/api/v1/auctions/au1/bids", method = POST)
        .withHeadersAndEntity(authHeader :: Nil, HttpEntity(`application/json`, body))

      Then("response should have 201 status")
      createBidRequest ~> auctionRoutes ~> check {
        status should ===(Created)
      }

      When("request to fetch auction by id with valid authentication token is sent to auction api")
      val fetchAuctionRequest = HttpRequest(uri = "/api/v1/auctions/au1", method = GET, headers = authHeader :: Nil)

      Then("response should have 200 status and contain created bid")
      fetchAuctionRequest ~> auctionRoutes ~> check {
        status should ===(OK)
        contentType should ===(`application/json`)
        entityAs[String] should ===(
          s"""{
             |"description":"",
             |"bids":[{"bidId":"$expectedBidId","bidder":"testuser","amount":10.0}],
             |"auctionId":"au1",
             |"minimumPrice":0.0,
             |"details":{},
             |"owner":"testuser2",
             |"title":"auction 1"
             |}""".stripMargin.filter(_ >= ' ')
        )
      }
    }

    "bid in existing auction with too small bid and with correct authentication token" in {
      Given("an auction exists in auction api and a bid with amount 15 for that auction exists")
      addAuctionWithBids("au1", "testuser2", "auction 1", Bid("some-bid", "anotheruser", BigDecimal(15d)) :: Nil)

      When("request to bid in auction with amount 10 is sent to auction api with valid authentication token")
      val authHeader = Authorization(OAuth2BearerToken("valid-token"))
      val body = BidRequest(BigDecimal(10.0d)).toJson.prettyPrint
      val createBidRequest = HttpRequest(uri = "/api/v1/auctions/au1/bids", method = POST)
        .withHeadersAndEntity(authHeader :: Nil, HttpEntity(`application/json`, body))

      Then("response should have 409 status")
      createBidRequest ~> auctionRoutes ~> check {
        status should ===(Conflict)
        contentType should ===(`application/json`)
        entityAs[String] should ===("""{"error":"your bid is not high enough"}""")
      }
    }


  }

}
