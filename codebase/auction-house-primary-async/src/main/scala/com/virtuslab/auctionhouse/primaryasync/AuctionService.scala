package com.virtuslab.auctionhouse.primaryasync

import java.sql.Timestamp
import java.time.LocalDateTime
import java.util.Objects.isNull
import java.util.UUID

import com.datastax.driver.core.querybuilder.QueryBuilder.{desc, insertInto, select, eq => equal}
import com.datastax.driver.core.utils.UUIDs
import com.datastax.driver.core.{ResultSet, Row, Session}
import com.typesafe.scalalogging.Logger
import com.virtuslab.TraceId
import com.virtuslab.cassandra.CassandraClient
import spray.json._

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future.{failed, successful}
import scala.concurrent.{ExecutionContext, Future}

trait AuctionService {

  // errors
  case class AuctionNotFound(auctionId: String) extends RuntimeException(auctionId)

  case class BidTooSmall(auctionId: String, highestBidAmount: BigDecimal) extends RuntimeException(s"$auctionId:$highestBidAmount")

  // request vm
  case class CreateAuctionRequest(category: String, title: String, description: String,
                                  minimumPrice: BigDecimal, details: JsObject) {
    def addOwner(owner: String): CreateAuction = CreateAuction(owner, category, title, description, minimumPrice, details)
  }

  case class BidRequest(amount: BigDecimal) {
    def enrich(bidder: String, auctionId: String): BidInAuction = BidInAuction(bidder, auctionId, amount)
  }

  // commands
  case class CreateAuction(owner: String, category: String, title: String,
                           description: String, minimumPrice: BigDecimal, details: JsObject)

  case class BidInAuction(bidder: String, auctionId: String, amount: BigDecimal)

  // response VMs
  case class CreatedAuction(auctionId: String)

  case class AuctionInfo(auctionId: String, createdAt: Long, owner: String, title: String, minimumPrice: BigDecimal)

  case class Auctions(category: String, auctions: List[AuctionInfo])

  case class AuctionResponse(category: String, auctionId: String, createdAt: Long,
                             owner: String, title: String,
                             description: String, minimumPrice: BigDecimal,
                             details: JsValue, bids: List[Bid])

  case class Bid(bidId: String, bidder: String, amount: BigDecimal)

  def createAuction(command: CreateAuction)(implicit traceId: TraceId): Future[String]

  def listAuctions(category: String)(implicit traceId: TraceId): Future[List[AuctionInfo]]

  def getAuction(auctionId: String)(implicit traceId: TraceId): Future[AuctionResponse]

  def bidInAuction(command: BidInAuction)(implicit traceId: TraceId): Future[Unit]
}

trait AuctionServiceImpl extends AuctionService {
  this: CassandraClient =>

  import com.virtuslab.AsyncUtils.Implicits._

  protected implicit def executionContext: ExecutionContext

  protected def logger: Logger

  private lazy val sessionFuture: Future[Session] = getSessionAsync

  def createAuction(command: CreateAuction)(implicit traceId: TraceId): Future[String] = {
    val auctionId = UUIDs.random()
    val timestamp = Timestamp.valueOf(LocalDateTime.now())
    val query = insertInto("microservices", "auctions")
      .value("category", command.category)
      .value("auction_id", auctionId)
      .value("created_at", timestamp)
      .value("owner", command.owner)
      .value("title", command.title)
      .value("description", command.description)
      .value("details", command.details.compactPrint)
      .value("minimum_price", command.minimumPrice.bigDecimal)

    for {
      session <- sessionFuture
      _ <- session.executeAsync(query).asScala
    } yield auctionId.toString
  }

  def listAuctions(category: String)(implicit traceId: TraceId): Future[List[AuctionInfo]] = {
    val query = select("auction_id", "created_at", "owner", "title", "minimum_price")
      .from("microservices", "auctions")
      .where(equal("category", category))
      .orderBy(desc("created_at"))
      .limit(15)

    def auctionInfoFromRow(row: Row) = AuctionInfo(
      row.getUUID("auction_id").toString,
      row.getTimestamp("created_at").getTime,
      row.getString("owner"),
      row.getString("title"),
      BigDecimal(row.getDecimal("minimum_price"))
    )

    for {
      session <- sessionFuture
      auctionInfoesFuture <- aggregateAll(session.executeAsync(query).asScala, ArrayBuffer.empty, auctionInfoFromRow)
    } yield auctionInfoesFuture
  }

  def getAuction(auctionId: String)(implicit traceId: TraceId): Future[AuctionResponse] = {
    val auctionIdUuid = UUID fromString auctionId

    val auctionQuery = select().all()
      .from("microservices", "auctions_view")
      .where(equal("auction_id", auctionIdUuid))
      .orderBy(desc("created_at"))
      .limit(1)

    val bidsQuery = select().all()
      .from("microservices", "bids")
      .where(equal("auction_id", auctionIdUuid))

    def transformAuctionResultSet(auctionRs: ResultSet, bids: List[Bid]): Future[AuctionResponse] = {
      Option(auctionRs.one())
        .map { row =>
          AuctionResponse(
            category = row.getString("category"),
            auctionId = row.getUUID("auction_id").toString,
            createdAt = row.getTimestamp("created_at").getTime,
            owner = row.getString("owner"),
            title = row.getString("title"),
            description = row.getString("description"),
            minimumPrice = BigDecimal(row.getDecimal("minimum_price")),
            details = row.getString("details").parseJson,
            bids = bids
          )
        }
        .fold[Future[AuctionResponse]](failed(AuctionNotFound(auctionId)))(successful)
    }

    for {
      session <- sessionFuture
      auctionRs <- session.executeAsync(auctionQuery).asScala
      bids <- aggregateAll(session.executeAsync(bidsQuery).asScala, ArrayBuffer.empty, transformBid)
      auction <- transformAuctionResultSet(auctionRs, bids)
    } yield auction
  }

  def bidInAuction(command: BidInAuction)(implicit traceId: TraceId): Future[Unit] = {
    val bidId = UUIDs.timeBased()
    val auctionId = UUID fromString command.auctionId
    val fetchExistingBidsQuery = select().all()
      .from("microservices", "bids")
      .where(equal("auction_id", auctionId))

    val insertBidQuery = insertInto("microservices", "bids")
      .value("auction_id", auctionId)
      .value("bid_id", bidId)
      .value("bidder", command.bidder)
      .value("amount", command.amount.bigDecimal)

    def verifyBidAmountIsHighest(fut: Future[ResultSet]): Future[Unit] = {
      aggregateAll(fut, ArrayBuffer.empty, row => BigDecimal(row.getDecimal("amount")))
        .flatMap { existingBidAmounts =>
          if (existingBidAmounts.isEmpty) successful(())
          else {
            val max = existingBidAmounts.max
            if (max > command.amount) failed(BidTooSmall(command.auctionId, max))
            else successful(())
          }
        }
    }

    for {
      session <- sessionFuture
      _ <- verifyBidAmountIsHighest(session.executeAsync(fetchExistingBidsQuery).asScala)
      _ <- session.executeAsync(insertBidQuery).asScala // todo maybe check if applied?
    } yield ()
  }

  private def aggregateAll[A](future: Future[ResultSet], xs: ArrayBuffer[A], transform: Row => A): Future[List[A]] = {
    future.flatMap { rs =>
      val remainingInPage = rs.getAvailableWithoutFetching
      val rsIterator = rs.iterator()

      (remainingInPage until 0 by -1).foreach { _ => xs append transform(rsIterator.next()) }

      if (isNull(rs.getExecutionInfo.getPagingState))
        successful(xs.toList)
      else
        aggregateAll(rs.fetchMoreResults().asScala, xs, transform)
    }
  }

  private def transformBid(row: Row) = Bid(
    bidId = row.getUUID("bid_id").toString,
    bidder = row.getString("bidder"),
    amount = BigDecimal(row.getDecimal("amount"))
  )

}
