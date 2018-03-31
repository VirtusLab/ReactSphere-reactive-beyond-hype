package com.virtuslab.auctionHouse.perfTests

import io.gatling.commons.stats.OK
import io.gatling.core.Predef.{Simulation, atOnceUsers, configuration, global, _}
import io.gatling.http.Predef.http

class AuctionHouseSimulation extends Simulation with RandomHelper {

  val errorHandler = new ErrorHandler

  val httpConf = http
    .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8") // Here are the common headers
    .acceptEncodingHeader("gzip, deflate")
    .acceptLanguageHeader("en-US,en;q=0.5")
    .userAgentHeader("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.8; rv:16.0) Gecko/20100101 Firefox/16.0")

  val accounts = new AccountsActions(errorHandler)
  val auctions = new AuctionsActions(errorHandler)

  val category = randCategory

  val createAuctionScenario = scenario("Create auction")
    .exec(accounts.createAccount())
    .exec(accounts.signIn)
    .exec(auctions.createAuction(category))

  val bidInAuctionScenario = scenario("Bid in auction")
    .exec(accounts.createAccount())
    .exec(accounts.signIn)
    .asLongAs(s => s(AuctionsActions.auctionsParam).asOption[AuctionsActions.Auctions]
      .map(_.auctions.isEmpty)
      .getOrElse(true) && s.status == OK) {
      exec(auctions.listAuctions(category))
    }.exec(auctions.getAuction)
    .exec(auctions.bidInAuction)
    .exec(auctions.getAuctionWithBids)


  setUp(
    createAuctionScenario.inject(atOnceUsers(1)).protocols(httpConf),
    bidInAuctionScenario.inject(atOnceUsers(1)).protocols(httpConf)
  ).assertions(
    global.successfulRequests.percent.gte(100)
  )

  after({
    if (errorHandler.errors.nonEmpty) {
      throw new RuntimeException(s"There were exception while running scenario:\n${errorHandler.errors.mkString("\n")}")
    }
  })
}