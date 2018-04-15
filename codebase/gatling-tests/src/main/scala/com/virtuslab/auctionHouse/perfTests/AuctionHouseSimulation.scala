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

  object InjectionMode extends Enumeration {
    type InjectionMode = Value
    val Heavyside, RampUp, InjectAndWait = Value
  }

  val systemVars = System.getenv().asScala
  val rampUpMax = systemVars.get("RAMP_UP_MAX").map(_.toInt).getOrElse(1)
  val rampUpDurationSecs = systemVars.get("RAMP_UP_TIME").map(_.toInt).getOrElse(60)
  val injectionMode = systemVars.get("INJECTION_MODE").flatMap { m =>
    InjectionMode.values.find(_.toString == m)
  }.getOrElse(InjectionMode.RampUp)


  val auctionCreatorsFeeder = (0 until rampUpMax).map { _ =>
    val category = randCategory
    Map(
      SessionConstants.username -> randStr,
      SessionConstants.password -> randStr,
      SessionConstants.category -> category,
      SessionConstants.createAuctionRequest -> CreateAuctionRequest(category, randStr, randStr, randPosNum,
        parse(s"""{"$randStr": "$randStr"}"""))
    )
  }.toArray.circular

  val biddersFeeder = (0 until rampUpMax).map { _ =>
    val category = randCategory
    Map(
      SessionConstants.username -> randStr,
      SessionConstants.password -> randStr,
      SessionConstants.category -> category
    )
  }.toArray.circular

  val httpConf = http
    .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8") // Here are the common headers
    .acceptEncodingHeader("gzip, deflate")
    .acceptLanguageHeader("en-US,en;q=0.5")
    .userAgentHeader("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.8; rv:16.0) Gecko/20100101 Firefox/16.0")

  val accounts = new AccountsActions(errorHandler)
  val auctions = new AuctionsActions(errorHandler)


  val createAuctionHeavysideScenario = scenario("Create auction")
    .pause(0, 1 second)
    .feed(auctionCreatorsFeeder)
    .exec(accounts.createAccount).exitHereIfFailed.pause(300 millis)
    .exec(accounts.signIn).exitHereIfFailed.pause(300 millis)
    .exec(auctions.createAuction)

  val bidInAuctionHeavysideScenario = scenario("Bid in auction")
    .pause(0, 1 second)
    .feed(biddersFeeder)
    .exec(accounts.createAccount).exitHereIfFailed.pause(300 millis)
    .exec(accounts.signIn).exitHereIfFailed.pause(300 millis)
    .asLongAs(s =>
      s(AuctionsActions.auctionsParam).asOption[AuctionsActions.Auctions]
        .map(_.auctions.isEmpty)
        .getOrElse(true) && s.status == OK) {
      exec(auctions.listAuctions)
        .pause(300 millis)
        .exec(_.set(SessionConstants.category, randCategory))
    }
    .exec(auctions.getAuction).exitHereIfFailed.pause(300 millis)
    .exec(auctions.bidInAuction).exitHereIfFailed.pause(300 millis)
    .exec(auctions.payForAuction)

  val createAuctionScenario = scenario("Create auction")
    .pause(0, 1 second)
    .feed(auctionCreatorsFeeder)
    .asLongAs(_(SessionConstants.createAccountResponse).asOption[Int] != Some(201)) {
      exec(accounts.createAccount).pause(1 second)
    }
    .during(rampUpDurationSecs seconds) {
      exec(accounts.signIn).pause(300 millis)
        .exec(auctions.createAuction)
    }

  val bidInAuctionScenario = scenario("Bid in auction")
    .pause(0, 1 second)
    .feed(biddersFeeder)
    .asLongAs(_(SessionConstants.createAccountResponse).asOption[Int] != Some(201)) {
      exec(accounts.createAccount).pause(1 second)
    }
    .during(rampUpDurationSecs seconds) {
      exec(accounts.signIn).pause(300 millis)
        .asLongAs(s =>
          s(AuctionsActions.auctionsParam).asOption[AuctionsActions.Auctions]
            .map(_.auctions.isEmpty)
            .getOrElse(true) && s.status == OK) {
          exec(auctions.listAuctions)
            .pause(300 millis)
            .exec(_.set(SessionConstants.category, randCategory))
        }
        .exec(auctions.getAuction).pause(300 millis)
        .exec(auctions.bidInAuction).pause(300 millis)
        .exec(auctions.payForAuction)
    }


  val basePause = rampUpDurationSecs seconds

  val injectAndWaitScenario =
    scenario("Inject and wait")
      .feed(auctionCreatorsFeeder)
      .exec(accounts.createAccount).exitHereIfFailed.pause(basePause)
      .exec(accounts.signIn).exitHereIfFailed.pause(basePause)
      .exec(auctions.createAuction).exitHereIfFailed.pause(basePause)
      .exec(auctions.listAuctions).exitHereIfFailed.pause(basePause)
      .asLongAs(s =>
      s(AuctionsActions.auctionsParam).asOption[AuctionsActions.Auctions]
        .map(_.auctions.isEmpty)
        .getOrElse(true) && s.status == OK) {
        exec(auctions.listAuctions)
          .pause(basePause)
          .exec(_.set(SessionConstants.category, randCategory))
      }
        .exec(auctions.getAuction).exitHereIfFailed.pause(basePause)
        .exec(auctions.bidInAuction).exitHereIfFailed.pause(basePause)
        .exec(auctions.payForAuction).inject(atOnceUsers(rampUpMax)).protocols(httpConf)

  val scenarios = injectionMode match {
    case InjectionMode.Heavyside => Seq(
      createAuctionHeavysideScenario.inject(heavisideUsers(rampUpMax).over(rampUpDurationSecs seconds)).protocols(httpConf),
      bidInAuctionHeavysideScenario.inject(heavisideUsers(rampUpMax).over(rampUpDurationSecs seconds)).protocols(httpConf)
    )
    case InjectionMode.RampUp => Seq(
      createAuctionScenario.inject(rampUsers(rampUpMax).over(rampUpDurationSecs seconds)).protocols(httpConf),
      bidInAuctionScenario.inject(rampUsers(rampUpMax).over(rampUpDurationSecs seconds)).protocols(httpConf)
    )
    case InjectionMode.InjectAndWait => Seq(injectAndWaitScenario)
  }


  setUp(scenarios: _*).assertions(
    global.successfulRequests.percent.gte(100)
  ).maxDuration(if(injectionMode == InjectionMode.InjectAndWait) (rampUpDurationSecs seconds) * 20 else (rampUpDurationSecs seconds) * 5)

  after({
    if (errorHandler.errors.nonEmpty) {
      if (throwOnFailure) {
        throw new RuntimeException(s"There were exception while running scenario:\n${errorHandler.errors.mkString("\n")}")
      }
      log.warn(s"There were exception while running scenario:\n${errorHandler.errors.mkString("\n")}")
    }
  })
}