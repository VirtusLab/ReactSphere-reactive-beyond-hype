package com.virtuslab.auctionHouse.perfTests

import com.typesafe.scalalogging.Logger
import com.virtuslab.Logging
import com.virtuslab.auctionHouse.perfTests.AuctionsActions.CreateAuctionRequest
import io.gatling.commons.stats.OK
import io.gatling.core.Predef.{Simulation, configuration, global, _}
import io.gatling.http.Predef.http
import org.json4s.jackson.JsonMethods.parse
import scala.collection.JavaConverters._

import scala.concurrent.duration._

class AuctionHouseSimulation extends Simulation with RandomHelper with Logging {

  override protected val log = Logger(getClass)
  val errorHandler = new ErrorHandler
  val throwOnFailure = false

  val systemVars = System.getenv().asScala
  val rampUpMax = systemVars.get("RAMP_UP_MAX").map(_.toInt).getOrElse(1)
  val rampUpDurationSecs = systemVars.get("RAMP_UP_TIME").map(_.toInt).getOrElse(60)


  val auctionCreatorsFeeder = (0 until rampUpMax).map { _ =>
    val category = randCategory
    Map(
      SessionConstants.username -> randStr,
      SessionConstants.password -> randStr,
      SessionConstants.category -> category,
      SessionConstants.createAuctionRequest -> CreateAuctionRequest(category, randStr, randStr, randPosNum,
        parse(s"""{"$randStr": "$randStr"}"""))
    )
  }.toArray

  val biddersFeeder = (0 until rampUpMax).map { _ =>
    val category = randCategory
    Map(
      SessionConstants.username -> randStr,
      SessionConstants.password -> randStr,
      SessionConstants.category -> category
    )
  }.toArray

  val httpConf = http
    .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8") // Here are the common headers
    .acceptEncodingHeader("gzip, deflate")
    .acceptLanguageHeader("en-US,en;q=0.5")
    .userAgentHeader("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.8; rv:16.0) Gecko/20100101 Firefox/16.0")

  val accounts = new AccountsActions(errorHandler)
  val auctions = new AuctionsActions(errorHandler)


  val createAuctionScenario = scenario("Create auction")
    .feed(auctionCreatorsFeeder)
    .exec(accounts.createAccount).exitHereIfFailed.pause(300 millis)
    .exec(accounts.signIn).exitHereIfFailed.pause(300 millis)
    .exec(auctions.createAuction)

  val bidInAuctionScenario = scenario("Bid in auction")
    .feed(biddersFeeder)
    .exec(accounts.createAccount).pause(300 millis)
    .exec(accounts.signIn).pause(300 millis)
    .asLongAs(s => s(AuctionsActions.auctionsParam).asOption[AuctionsActions.Auctions]
      .map(_.auctions.isEmpty)
      .getOrElse(true) && s.status == OK) {
        exec(auctions.listAuctions)
          .pause(300 millis)
          .exec(_.set(SessionConstants.category, randCategory))
    }
.exec(auctions.getAuction).pause(300 millis)
    .exec(auctions.bidInAuction).pause(300 millis)
    .exec(auctions.payForAuction)


  setUp(
    createAuctionScenario.inject(heavisideUsers(rampUpMax).over(rampUpDurationSecs seconds)).protocols(httpConf),
    bidInAuctionScenario.inject(heavisideUsers(rampUpMax).over(rampUpDurationSecs seconds)).protocols(httpConf)
  ).assertions(
    global.successfulRequests.percent.gte(100)
  ).maxDuration((rampUpDurationSecs * 4) seconds)

  after({
    if (errorHandler.errors.nonEmpty) {
      if(throwOnFailure) {
        throw new RuntimeException(s"There were exception while running scenario:\n${errorHandler.errors.mkString("\n")}")
      }
      log.warn(s"There were exception while running scenario:\n${errorHandler.errors.mkString("\n")}")
    }
  })
}