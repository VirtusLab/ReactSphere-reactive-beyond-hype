package com.virtuslab.auctionHouse.perfTests

import java.util.concurrent.ConcurrentLinkedQueue

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import org.slf4j.LoggerFactory


class AccountsActions(errorHandler: ErrorHandler) extends BaseActions(errorHandler) {


  protected val logger = LoggerFactory.getLogger(getClass)

  val scenarioErrors = new ConcurrentLinkedQueue[String]()
  def url(path: String) =  s"http://${Config.identityServiceContactPoint}/api/${Config.apiVersion}/$path"

  def createAccount = {
    http("Account creation")
      .post(url("accounts"))
      .header("Content-Type", "application/json")
      .body(StringBody("""{"username": "${username}", "password" : "${password}" }"""))
      .check(
        status.is(201).saveAs(SessionConstants.createAccountResponse)
      )
  }

  def signIn = {
    http("sign in")
      .post(url("sign-in"))
      .header("Content-Type", "application/json")
      .body(StringBody(SessionParams.signInRequestTemplate))
      .check(
        status.in(200),
        jsonPath("$.token").saveAs(SessionConstants.token)
      )
  }
}
