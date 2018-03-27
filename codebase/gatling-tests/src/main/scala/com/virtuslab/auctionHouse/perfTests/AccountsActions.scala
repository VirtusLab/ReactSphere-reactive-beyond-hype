package com.virtuslab.auctionHouse.perfTests

import java.util.concurrent.ConcurrentLinkedQueue

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import org.slf4j.LoggerFactory


class AccountsActions(errorHandler: ErrorHandler) extends BaseActions(errorHandler) {


  protected val logger = LoggerFactory.getLogger(getClass)


  val baseUrl = s"http://${Config.serverHostPort}/api/${Config.apiVersion}/"

  val scenarioErrors = new ConcurrentLinkedQueue[String]()




  def createAccount(username: String = randStr, password: String = randStr) = {
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

  def signIn = {
    http("sign in")
      .post("sign-in")
      .header("Content-Type", "application/json")
      .body(StringBody(SessionParams.signInRequestTemplate))
      .check(
        status.in(200),
        jsonPath("$.token").saveAs(SessionParams.token)
      )
  }
}
