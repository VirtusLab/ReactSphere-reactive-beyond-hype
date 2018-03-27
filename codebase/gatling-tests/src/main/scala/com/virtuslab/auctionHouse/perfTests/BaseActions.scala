package com.virtuslab.auctionHouse.perfTests

import io.gatling.core.Predef._
import io.gatling.core.session.Expression
import io.gatling.http.request.builder.HttpRequestBuilder


abstract class BaseActions(errorHandler: ErrorHandler) extends RandomHelper {

  implicit val formats = org.json4s.DefaultFormats


  object SessionParams {

    case class Account(username: String, password: String)

    val account = "account"
    val token = "token"

    val signInRequestTemplate: Expression[String] = (session: Session) => {
      session(account).asOption[Option[Account]].flatten
        .map(account => s"""{"username": "${account.username}", "password" : "${account.password}"}""")
        .getOrElse(errorHandler.raiseError("Account not found in gatling session"))
    }

    val authHeaderValue: Expression[String] = (session: Session) => {
      session(token).asOption[String]
        .map(token => s"bearer $token")
        .getOrElse(errorHandler.raiseError("Token not found in gatling session"))
    }
  }

  implicit class HttpRequestBuilderWrapper(httpBuilder: HttpRequestBuilder) {

    def withDefaultHeaders(): HttpRequestBuilder = {
      httpBuilder.header("Content-Type", "application/json")
    }

    def withAuthHeaders(): HttpRequestBuilder = {
      httpBuilder
        .withDefaultHeaders()
        .header("Authorization", SessionParams.authHeaderValue)
    }
  }
}
