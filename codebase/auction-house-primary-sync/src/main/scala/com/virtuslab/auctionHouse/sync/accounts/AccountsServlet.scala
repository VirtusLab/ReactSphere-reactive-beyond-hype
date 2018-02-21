package com.virtuslab.auctionHouse.sync.accounts

import com.virtuslab.auctionHouse.sync.accounts.AccountService.DuplicatedEntityException
import com.virtuslab.auctionHouse.sync.commons.ServletModels.ErrorResponse
import com.virtuslab.identity.CreateAccountRequest
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.json.JacksonJsonSupport
import org.scalatra.{BadRequest, Created, ScalatraServlet}

import scala.util.Try

class AccountsServlet extends ScalatraServlet with JacksonJsonSupport {
  override protected implicit def jsonFormats: Formats = DefaultFormats

  lazy val accountService = new AccountService()

  post("/") {
    val accountRequest = parsedBody.extract[CreateAccountRequest]
    Try(accountService.createAccount(accountRequest))
      .map(_ => Created())
      .recover { case e: DuplicatedEntityException  => BadRequest(ErrorResponse(e.getMessage)) }
      .get
  }
}
