package com.virtuslab.auctionhouse.primaryasync

import akka.actor.ActorSystem
import akka.stream.Materializer
import com.datastax.driver.core.utils.UUIDs
import com.typesafe.scalalogging.Logger
import com.virtuslab.auctionhouse.cassandra.CassandraIntegrationTest
import com.virtuslab.{CassandraQueriesMetrics, TraceId}
import com.virtuslab.payments.payments.PaymentRequest
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{GivenWhenThen, Matchers, OptionValues, WordSpec}
import spray.json.{JsArray, JsObject, JsString}

import scala.concurrent.ExecutionContext.Implicits
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

class AuctionServiceIntegrationTest extends WordSpec with Matchers with ScalaFutures with GivenWhenThen
  with OptionValues with CassandraIntegrationTest with AuctionServiceImpl with CassandraQueriesMetrics {

  implicit val executionContext: ExecutionContext = Implicits.global
  implicit val traceId: TraceId = TraceId(UUIDs.random().toString)

  override protected def logger: Logger = Logger(getClass)

  override protected implicit def system: ActorSystem = ???

  override protected implicit def materializer: Materializer = ???

  override protected def createBill(billRequest: PaymentRequest, token: String)(implicit traceId: TraceId): Future[Unit] = Future.successful()

  "Auction service" should {

    "create auctions" in {
      When("auction is created")
      val createdAuction = createAuction {
        CreateAuction("testuser", "test-category", "test auction", "test item description", BigDecimal(20d), JsObject())
      }

      Then("auction should exist in database")
      Await.result(createdAuction, 1.second)

      val auctionRowMaybe = Option(fetch("auctions").one())
      auctionRowMaybe shouldBe defined

      val Some(auctionRow) = auctionRowMaybe
      auctionRow.getString("owner") should be("testuser")
      auctionRow.getString("title") should be("test auction")
      auctionRow.getString("description") should be("test item description")
      BigDecimal(auctionRow.getDecimal("minimum_price")) should be(BigDecimal(20d))
    }

    "return empty list if no auctions exist" in {
      Given("no auctions exist")
      When("auctions are listed")
      val auctions = Await.result(listAuctions("test-category"), 1.second)

      Then("no auctions should be returned")
      auctions.length should be(0)
    }

    "list existing auctions" in {
      Given("three auctions exists in database")
      Await.result(for {
        _ <- createAuction {
          CreateAuction("testuser", "test-category", "test auction", "test item description", BigDecimal(20d), JsObject())
        }
        _ <- createAuction {
          CreateAuction("testuser2", "test-category", "another auction", "another item description", BigDecimal(23d), JsObject("test" -> JsString("yay")))
        }
        _ <- createAuction {
          CreateAuction("testuser", "test-category", "second auction", "cool description", BigDecimal(27d), JsObject("arbitrary" -> JsArray()))
        }
      } yield (), 2.seconds)

      When("auctions are listed")
      val auctions = Await.result(listAuctions("test-category"), 1.second)

      Then("three existing auctions should be returned in correct order")
      auctions.length should be(3)
      val vec = auctions.toVector
      vec(0).minimumPrice should be(BigDecimal(27d))
      vec(1).minimumPrice should be(BigDecimal(23d))
      vec(2).minimumPrice should be(BigDecimal(20d))
      vec(0).createdAt should be > vec(1).createdAt
      vec(1).createdAt should be > vec(2).createdAt
    }

    "fail to fetch auctions that don't exist" in {
      When("non-existent auction is fetched")
      val nonExistentAuctionId = UUIDs.random.toString
      val auctionFuture = getAuction(nonExistentAuctionId)

      Then("auction not found error should occur")
      intercept[AuctionNotFound] {
        Await.result(auctionFuture, 1.second)
      }
    }

    "fetch existing auctions without any bids" in {
      Given("auction exists")
      val auctionId = Await.result(createAuction {
        CreateAuction(
          "testuser", "test-category", "test auction", "test item description",
          BigDecimal(20d), JsObject("something" -> JsString("else"))
        )
      }, 1.second)

      When("auction is fetched")
      val auction = Await.result(getAuction(auctionId), 1.second)

      Then("auction data should be correct")
      auction.category should be("test-category")
      auction.owner should be("testuser")
      auction.title should be("test auction")
      auction.description should be("test item description")
      auction.minimumPrice should be(BigDecimal(20))
      auction.details should be(JsObject("something" -> JsString("else")))
      auction.bids should be(List.empty)
    }

    "bid in auctions" in {
      Given("auction exists and it's id is known")
      val auctionId = Await.result(createAuction {
        CreateAuction(
          "testuser", "test-category", "test auction", "test item description",
          BigDecimal(20d), JsObject("something" -> JsString("else"))
        )
      }, 1.second)

      When("bid is created for that auction")
      Await.result(bidInAuction {
        BidInAuction("test-bidder", auctionId, BigDecimal(25))
      }, 1.second)

      Then("bid for given auction exists in database")
      val auction = Await.result(getAuction(auctionId), 1.second)
      auction.bids.length should be(1)
      auction.bids.head.amount should be(BigDecimal(25))
      auction.bids.head.bidder should be("test-bidder")
    }

    "fail when higher bid already exists for given auction" in {
      Given("auction with a bid exists and it's id is known")
      val auctionId = Await.result(createAuction {
        CreateAuction(
          "testuser", "test-category", "test auction", "test item description",
          BigDecimal(20d), JsObject("something" -> JsString("else"))
        )
      }, 1.second)
      Await.result(bidInAuction {
        BidInAuction("test-bidder", auctionId, BigDecimal(25))
      }, 1.second)

      When("bid with amount lower than current maximum bid is created for that auction")
      val bidFuture = bidInAuction {
        BidInAuction("test-bidder-2", auctionId, BigDecimal(22))
      }

      Then("bid too small error occurs")
      intercept[BidTooSmall] {
        Await.result(bidFuture, 1.second)
      }
    }

    "create bid for auctions that already have lower bids" in {
      Given("auction with a bid exists and it's id is known")
      val auctionId = Await.result(createAuction {
        CreateAuction(
          "testuser", "test-category", "test auction", "test item description",
          BigDecimal(20d), JsObject("something" -> JsString("else"))
        )
      }, 1.second)
      Await.result(bidInAuction {
        BidInAuction("test-bidder", auctionId, BigDecimal(25))
      }, 1.second)

      When("bid with amount higher than current maximum bid is created for that auction")
      Await.result(bidInAuction {
        BidInAuction("test-bidder-2", auctionId, BigDecimal(30))
      }, 1.second)

      Then("bid should be successfully created as the highest bid")
      Then("bid for given auction exists in database")
      val auction = Await.result(getAuction(auctionId), 1.second)
      auction.bids.length should be(2)
      val sortedBids = auction.bids.sortBy(-_.amount)
      sortedBids.head.amount should be(BigDecimal(30))
      sortedBids.head.bidder should be("test-bidder-2")
    }

    "process paying for auctions when bidder bid is highest" in {
      Given("auction with a bid exists and it's id is known")
      val auctionId = Await.result(createAuction {
        CreateAuction(
          "testuser", "test-category", "test auction", "test item description",
          BigDecimal(20d), JsObject("something" -> JsString("else"))
        )
      }, 1.second)
      Await.result(bidInAuction {
        BidInAuction("test-bidder", auctionId, BigDecimal(25))
      }, 1.second)

      When("bid with amount higher than current maximum bid is created for that auction")
      Await.result(bidInAuction {
        BidInAuction("test-bidder-2", auctionId, BigDecimal(30))
      }, 1.second)

      Await.result(payForAuction(auctionId, "test-bidder-2", "some-token"), 1.second)
      intercept[NotActionWinner] {
        Await.result(payForAuction(auctionId, "test-bidder-1", "some-token"), 1.second)
      }
      intercept[NotActionWinner] {
        Await.result(payForAuction(auctionId, "test-bidder-3", "some-token"), 1.second)
      }
    }

  }

}
