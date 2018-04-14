package com.virtuslab.auctionHouse.sync.auctions



class AuctionsService extends TraceIdSupport with Logging with HeadersSupport with CassandraQueriesMetrics {

  override val log = Logger(getClass.toString)

  lazy val auctionsMapper: Mapper[Auction] = SessionManager.mapper(classOf[Auction])
  lazy val accountsMapper: Mapper[Account] = SessionManager.mapper(classOf[Account])
  lazy val auctionsViewMapper: Mapper[AuctionView] = SessionManager.mapper(classOf[AuctionView])
  lazy val bidsMapper: Mapper[Bid] = SessionManager.mapper(classOf[Bid])
  lazy val session: Session = SessionManager.session

  private val categoriesSet = Categories.toSet
  private implicit val jsonFormats: Formats = DefaultFormats

  private val billingUrl = s"http://${Config.billingServiceContactPoint}/api/v1/billing"
  log.info(s"Billing url is: ${billingUrl}")

  private def assertCategory(category: String): Unit = {
    if (!categoriesSet.contains(category)) {
      throw new InvalidCategoryException(s"Invalid category: $category")
    }
  }

  def listAuctions(category: String): Auctions = {
    assertCategory(category)
    val auctions = usingCassandra(1) {
      auctionsMapper.map(session.execute(QueryBuilder.select().all().from("auctions")
        .where(QueryBuilder.eq("category", category)).limit(10)))
        .all().asScala.toList
    }
    Auctions(category, auctions)
  }

  private def accountExists(username: String): Boolean = accountsMapper.getOption(username).isDefined

  def createAuction(auctionRequest: CreateAuctionRequest, owner: String): UUID = {
    assertCategory(auctionRequest.category)
    if (!accountExists(owner)) {
      throw new UnknownEntityException(s"Cannot find owner: $owner")
    }
    val auction = new Auction(auctionRequest, owner)
    usingCassandra(1) { auctionsMapper.save(auction) }
    auction.auction_id
  }

  def getAuction(id: UUID): AuctionViewResponse = {
    usingCassandra(2) {
      auctionsViewMapper.map(session.execute(QueryBuilder.select().all().from("auctions_view")
        .where(QueryBuilder.eq("auction_id", id)))).asScala.headOption
        .map { auction =>
          val bids = bidsMapper.map(session.execute(QueryBuilder.select().all().from("bids")
            .where(QueryBuilder.eq("auction_id", id)))).asScala.toSeq
          AuctionViewResponse(auction, bids)
        }.getOrElse(throw new EntityNotFoundException(s"Auction id = $id cannot be found"))
    }
  }

  private def auctionExists(auctionId: UUID): Boolean = {
    usingCassandra(1) {
      1 == session.execute(QueryBuilder.select().countAll().from("auctions_view")
        .where(QueryBuilder.eq("auction_id", auctionId))).one().get(0, classOf[Long])
    }
  }

  def bidInAuction(auctionId: UUID, bidValue: BigDecimal, bidder: String): Unit = {
    if(!auctionExists(auctionId)) throw new UnknownEntityException(s"Cannot find account: $bidder")
    if (!accountExists(bidder)) throw new UnknownEntityException(s"Cannot find account: $bidder")
    val isMaxBid = usingCassandra(1) {
      bidsMapper.map(session.execute(QueryBuilder.select().all().from("bids")
        .where(QueryBuilder.eq("auction_id", auctionId)))).asScala.forall(b => BigDecimal(b.amount) < bidValue)
    }
    if (isMaxBid) {
      usingCassandra(1) {
        bidsMapper.save(new Bid(auctionId, UUIDs.timeBased(), bidder, bidValue.bigDecimal))
      }
    } else {
      throw new InvalidBidException("Bid value is not big enough")
    }
  }

  def payForAuction(auctionId: String, bidder: String, token: String)(implicit traceId: TraceId): Unit = {
    val auction = getAuction(UUID.fromString(auctionId))
    val bidsOrder = Ordering.by((_: ServletModels.Bid).amount)
    val maxBid = auction.bids.reduceOption(bidsOrder.max).filter(_.bidder == bidder)
      .getOrElse(throw new NotAuctionWinnerException(s"User $bidder is not winner of auction $auctionId"))

    val body = write(PaymentRequest(bidder, auction.owner, maxBid.amount))
    val response = Http(billingUrl)
      .headers(traceHeaders ++ authHeaders(Some(AuthToken(token))))
      .postData(body)
      .asString

    if(response.code == 200) {
      val transactionId = response.body
      log.info(s"Success payment fulfilled for user: $bidder, transactionId: ${transactionId}")
    } else {
      log.error(s"Billing request failed. Code: ${response.code}, msg: ${response.body}")
      throw new AuctionFinalizationException(s"Failed to finalize auction  ${response.body}")
    }
  }
}

object AuctionsService {

  class InvalidCategoryException(msg: String) extends RuntimeException(msg)

  class InvalidBidException(msg: String) extends RuntimeException(msg)

  class NotAuctionWinnerException(msg: String) extends RuntimeException(msg)

  class AuctionFinalizationException(msg: String) extends RuntimeException(msg)

  class UnknownEntityException(msg: String) extends RuntimeException(msg)

}
