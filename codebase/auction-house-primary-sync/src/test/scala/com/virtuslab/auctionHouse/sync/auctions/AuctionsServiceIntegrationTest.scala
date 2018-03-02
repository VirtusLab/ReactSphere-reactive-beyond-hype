package com.virtuslab.auctionHouse.sync.auctions

import java.util.{Date, UUID}

import com.datastax.driver.core.utils.UUIDs
import com.datastax.driver.mapping.Mapper
import com.virtuslab.auctionHouse.sync.auctions.AuctionsService.{InvalidCategoryException, UnknownEntityException}
import com.virtuslab.auctionHouse.sync.cassandra._
import com.virtuslab.auctionHouse.sync.commons.ServletModels.CreateAuctionRequest
import com.virtuslab.auctionhouse.cassandra.CassandraIntegrationTest
import com.virtuslab.auctions.Categories
import org.json4s.jackson.JsonMethods.parse
import org.scalatest.{Matchers, WordSpec}

class AuctionsServiceIntegrationTest extends WordSpec with CassandraIntegrationTest with Matchers {

  val sessionManager = new SessionManager {
    override lazy val session = getSession
  }

  val auctionsService = new AuctionsService {
    override lazy val auctionsMapper: Mapper[Auction] = sessionManager.mapper(classOf[Auction])
    override lazy val accountsMapper = sessionManager.mapper(classOf[Account])
    override lazy val auctionsViewMapper = sessionManager.mapper(classOf[AuctionView])
    override lazy val bidsMapper = sessionManager.mapper(classOf[Bid])
    override lazy val session = sessionManager.session
  }

  private def randomAuction = new Auction(Categories.head, new java.util.Date(),
    UUID.randomUUID(), "a", "a", "a", "a", new java.math.BigDecimal(0))

  "Listing auctions" should {
    "return empty list" when {
      "there is no auctions" in {
        assert(auctionsService.listAuctions(Categories.head).auctions.isEmpty)
      }
    }

    "return non empty list" when {
      "there are auctions" in {
        auctionsService.auctionsMapper.save(randomAuction)
        auctionsService.auctionsMapper.save(randomAuction)
        auctionsService.listAuctions(Categories.head).auctions.size should equal(2)
        auctionsService.listAuctions(Categories.last).auctions.isEmpty should equal(true)
      }
    }

    "limit results to 10" when {
      "amount is > 10" in {
        (0 until 12).foreach(_ =>
          auctionsService.auctionsMapper.save(randomAuction)
        )
        auctionsService.listAuctions(Categories.head).auctions.size should equal(10)
      }
    }

    "throws exception" when {
      "category is unknown" in {
        intercept[InvalidCategoryException] {
          auctionsService.listAuctions("foo")
        }
      }
    }
  }


  "Creating auctions" should {
    "successfully create auction" when {
      "correct data is provided" in {
        val req = CreateAuctionRequest(Categories.head, "t1", "desc1", 1, parse("""{"details": "foo"}"""))
        val beforeCreation = new Date().getTime
        auctionsService.accountsMapper.save(new Account("o1", "p1"))
        val id = auctionsService.createAuction(req, "o1")
        val auction = auctionsService.getAuction(id)
        auction.auctionId should equal(id.toString)
      }
    }

    "throw exception" when {
      "owner is invalid" in {
        val req = CreateAuctionRequest(Categories.head, "t1", "desc1", 1, parse("""{"details": "foo"}"""))
        intercept[UnknownEntityException] {
          auctionsService.createAuction(req, "foo_owner")
        }
      }
    }
  }

  "Getting auction by id" should {
    "return action with bids" when {
      "correct id is provided" in {
        val req = CreateAuctionRequest(Categories.head, "t1", "desc1", 1, parse("""{"details": "foo"}"""))
        auctionsService.accountsMapper.save(new Account("o1", "p1"))
        val auction1Id = auctionsService.createAuction(req, "o1")
        auctionsService.bidsMapper.save(new Bid(auction1Id, UUIDs.timeBased(), "o1", new java.math.BigDecimal(10)))
        auctionsService.bidsMapper.save(new Bid(auction1Id, UUIDs.timeBased(), "o1", new java.math.BigDecimal(10)))
        val auction2Id = auctionsService.createAuction(req.copy(title = "t2"), "o1")
        val a1 = auctionsService.getAuction(auction1Id)
        a1.title should equal("t1")
        a1.bids.size should equal(2)
        val a2 = auctionsService.getAuction(auction2Id)
        a2.title should equal("t2")
        a2.bids.size should equal(0)
      }
    }
  }

  "Bidding in auction" should {
    "executes successfully" when {
      "bid is highest one in auction" in {
        val req = CreateAuctionRequest(Categories.head, "t1", "desc1", 1, parse("""{"details": "foo"}"""))
        auctionsService.accountsMapper.save(new Account("o1", "p1"))
        val auctionId = auctionsService.createAuction(req, "o1")
        auctionsService.bidInAuction(auctionId, 5, "o1")
        auctionsService.bidInAuction(auctionId, 6, "o1")
        auctionsService.getAuction(auctionId).bids.size should equal(2)
      }
    }

    "throw exception" when {
      "owner is invalid" in {
        val req = CreateAuctionRequest(Categories.head, "t1", "desc1", 1, parse("""{"details": "foo"}"""))
        auctionsService.accountsMapper.save(new Account("o1", "p1"))
        val auctionId = auctionsService.createAuction(req, "o1")
        intercept[UnknownEntityException] {
          auctionsService.bidInAuction(auctionId, 5, "o2")
        }
      }

      "auction id is invalid" in {
        auctionsService.accountsMapper.save(new Account("o1", "p1"))
        intercept[UnknownEntityException] {
          auctionsService.bidInAuction(UUID.randomUUID(), 5, "o2")
        }
      }
    }
  }
}
