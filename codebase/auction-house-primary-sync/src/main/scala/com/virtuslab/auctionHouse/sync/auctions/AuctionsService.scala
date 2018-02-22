package com.virtuslab.auctionHouse.sync.auctions

import com.datastax.driver.core.querybuilder.QueryBuilder
import com.virtuslab.auctionHouse.sync.cassandra.{Auction, SessionManager}

import scala.collection.JavaConverters._

class AuctionsService {
  lazy val auctionsMapper = SessionManager.mapper(classOf[Auction])
  lazy val session = SessionManager.session

  def listAuctions: Seq[AuctionListRow] = {
    auctionsMapper.map(session.execute(QueryBuilder.select().all().from("auctions").limit(10)))
      .all().asScala.map(AuctionListRow(_))
  }
}
