package com.virtuslab.auctionHouse.perfTests

import java.util.concurrent.ConcurrentLinkedQueue

import com.danielasfregola.randomdatagenerator.RandomDataGenerator
import com.virtuslab.auctions.Categories
import io.gatling.core.Predef.{global, _}
import io.gatling.core.action.builder.ActionBuilder
import io.gatling.core.session.Expression
import io.gatling.core.structure.ScenarioBuilder
import io.gatling.http.Predef._
import org.json4s.JValue
import org.json4s.jackson.JsonMethods.parse
import org.json4s.jackson.Serialization.write
import org.scalacheck.{Arbitrary, Gen}
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._

class CreateAuctionScenario extends Simulation with RandomDataGenerator {

  protected val logger = LoggerFactory.getLogger(getClass)

  implicit val arbitraryStr: Arbitrary[String] = Arbitrary(Gen.alphaNumStr)
  val arbitraryCategory: Arbitrary[String] = Arbitrary(Gen.oneOf(Categories))

  implicit val formats = org.json4s.DefaultFormats


  def randStr = random[String]
  def randCategory = random[String](arbitraryCategory)
  def randAuction = CreateAuctionRequest(randCategory, randStr, randStr, random[BigDecimal],
    parse(s"""{"$randStr": "$randStr"}"""))

  case class CreateAuctionRequest(category: String, title: String, description: String, minimumPrice: BigDecimal,
                                  details: JValue)

  val baseUrl = s"http://${Config.serverHostPort}/api/${Config.apiVersion}/"

  val scenarioErrors = new ConcurrentLinkedQueue[String]()


  object SessionParams {

    case class Account(username: String, password: String)

    val account = "account"
    val token = "token"

    private def raiseError(msg: String) = {
      scenarioErrors.add(msg)
      throw new RuntimeException(msg)
    }

    val signInRequestTemplate: Expression[String] = (session: Session) => {
      session(account).asOption[Option[Account]].flatten
        .map(account => s"""{"username": "${account.username}", "password" : "${account.password}"}""")
        .getOrElse(raiseError("Account not found in gatling session"))
    }

    val authHeaderValue: Expression[String] = (session: Session) => {
      session(token).asOption[String]
        .map(token => s"bearer $token")
        .getOrElse(raiseError("Token not found in gatling session"))
    }
  }

  implicit class ScenarioBuilderWrapper(scenarioBuilder: ScenarioBuilder) {
    def execWithErrorCheck(actionBuilder: ActionBuilder): ScenarioBuilder = {
      scenarioBuilder
        .exec(actionBuilder)
        .exec(session => if (scenarioErrors.isEmpty) session else session.markAsFailed)
        .exitHereIfFailed
    }
  }


  val httpConf = http
    .baseURL(baseUrl) // Here is the root for all relative URLs
    .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8") // Here are the common headers
    .acceptEncodingHeader("gzip, deflate")
    .acceptLanguageHeader("en-US,en;q=0.5")
    .userAgentHeader("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.8; rv:16.0) Gecko/20100101 Firefox/16.0")


  private def createAccount(username: String, password: String) = {
    println(s"Creating account username=$username password=$password")
    http("Account creation")
      .post("accounts")
      .header("Content-Type", "application/json")
      .body(StringBody(s"""{"username": "${username}", "password" : "${password}"}"""))
      .check(
        status.is(201),
        status.transform(_ match {
          case 201 => Some(SessionParams.Account(username, password))
          case _ => None
        }).saveAs(SessionParams.account))
  }

  private def signIn = {
    http("sign in")
      .post("sign-in")
      .header("Content-Type", "application/json")
      .body(StringBody(SessionParams.signInRequestTemplate))
      .check(
        status.in(200),
        jsonPath("$.token").saveAs(SessionParams.token)
      )
  }


  private def createAuction = {
    http("create auction")
      .post("auctions")
      .header("Content-Type", "application/json")
      .header("Authorization", SessionParams.authHeaderValue)
      .body(StringBody(write(randAuction)))
      .check(
        status.in(201)
      )
  }


  val scn = scenario("Create auction")
    .tryMax(5) {
      exec(createAccount(randStr, randStr))
    }.execWithErrorCheck(signIn)
    .execWithErrorCheck(createAuction)

  setUp(scn.inject(atOnceUsers(1)).protocols(httpConf)).assertions(
    global.successfulRequests.percent.gte(100),
  )

  after({
    if (scenarioErrors.asScala.nonEmpty) {
      throw new RuntimeException(s"There were exception while running scenario:\n${scenarioErrors.asScala.mkString("\n")}")
    }
  })
}
