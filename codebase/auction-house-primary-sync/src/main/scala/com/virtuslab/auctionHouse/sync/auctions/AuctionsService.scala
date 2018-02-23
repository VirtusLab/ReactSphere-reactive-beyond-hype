package com.virtuslab.auctionHouse.sync.auctions

import com.datastax.driver.core.querybuilder.QueryBuilder
import com.virtuslab.auctionHouse.sync.auctions.AuctionsService.InvalidCategoryException
import com.virtuslab.auctionHouse.sync.cassandra.{Auction, Categories, SessionManager}
import com.virtuslab.auctionHouse.sync.commons.ServletModels.Auctions

import scala.collection.JavaConverters._

class AuctionsService {
  lazy val auctionsMapper = SessionManager.mapper(classOf[Auction])
  lazy val session = SessionManager.session

  private val categoriesSet = Categories.toSet

  def listAuctions(category: String): Auctions = {
    if(!categoriesSet.contains(category)) {
      throw new InvalidCategoryException(s"Invalid category: $category")
    }
    val auctions = auctionsMapper.map(session.execute(QueryBuilder.select().all().from("auctions")
      .where(QueryBuilder.eq("category", category)).limit(10)))
      .all().asScala.toList
    Auctions(category, auctions)
  }
}
object AuctionsService {
  class InvalidCategoryException(msg: String) extends RuntimeException(msg)
}
