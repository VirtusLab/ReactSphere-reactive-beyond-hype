package com.virtuslab.auctionHouse.sync

import java.util.{Date, UUID}

import com.datastax.driver.mapping.annotations.{PartitionKey, Table}
import com.virtuslab.auctionHouse.sync.commons.ServletModels.CreateAuctionRequest
import com.virtuslab.identity.CreateAccountRequest
import org.json4s.jackson.JsonMethods.{compact, render}

import scala.annotation.meta.field

package object cassandra {

  import com.github.t3hnar.bcrypt._

  @Table(name = "accounts")
  class Account(@(PartitionKey @field) val username: String, val password: String) {

    def this() {
      this(null, null)
    }
    def this(req: CreateAccountRequest) {
      this(req.username, req.password.bcrypt)
    }

    def validatePassword(decryptedPass: String): Boolean = decryptedPass isBcrypted password
  }


  @Table(name = "tokens")
  class Token(@(PartitionKey @field) val token: String, val username: String, val expires_at: Date) {
    def this() {
      this(null, null, null)
    }
  }

  @Table(name = "auctions")
  class Auction(@(PartitionKey @field)(0) val category: String, @(PartitionKey @field)(1) val created_at: Date,
                @(PartitionKey @field)(2) val auction_id: UUID, val owner: String, val title: String,
                val description: String, val details: String, val minimum_price: java.math.BigDecimal) {
    def this() {
      this(null, null, null, null, null, null, null, null)
    }

    def this(a: CreateAuctionRequest,  owner: String) = {
      this(a.category, new Date(), UUID.randomUUID(), owner, a.title, a.description, compact(render(a.details)),
        a.minimumPrice.bigDecimal)
    }

    def id = AuctionId(category, created_at.getTime, auction_id)
  }

  @Table(name = "auctions_view")
  class AuctionView(@(PartitionKey @field)(0) val auction_id: UUID, @(PartitionKey @field)(1) val created_at: Date,
                    @(PartitionKey @field)(2) val category: String, val owner: String, val title: String,
                    val description: String, val details: String, val minimum_price: java.math.BigDecimal) {
    def this() {
      this(null, null, null, null, null, null, null, null)
    }
  }
  case class AuctionId(category: String, createdAt: Long, auctionId: UUID) {
    def idString = Seq(category, createdAt.toString, auctionId.toString).mkString(";")
  }

  @Table(name = "bids")
  class Bid(@(PartitionKey @field)(0) val auction_id: UUID, @(PartitionKey @field)(1) val bid_id: UUID,
            val bidder: String, val amount: java.math.BigDecimal) {
    def this() {
      this(null, null, null, null)
    }
  }

  val Categories = Vector(
    "motorization",
    "garden",
    "furniture",
    "home appliances",
    "electronics",
    "pets & animals",
    "clothing",
    "groceries",
    "health & beauty"
  )
}