package com.virtuslab.auctionHouse.sync.accounts

import com.virtuslab.auctionHouse.sync.BaseServlet
import com.virtuslab.auctionHouse.sync.accounts.AccountService.DuplicatedEntityException
import com.virtuslab.auctionHouse.sync.commons.ServletModels.ErrorResponse
import com.virtuslab.identity.CreateAccountRequest
import org.scalatra.{BadRequest, Created}

import scala.util.Try

class AccountsServlet extends BaseServlet {

  lazy val accountService = new AccountService()

  post("/") {
    val accountRequest = parsedBody.extract[CreateAccountRequest]
    Try(accountService.createAccount(accountRequest))
      .map(_ => Created())
      .recover { case e: DuplicatedEntityException  => BadRequest(ErrorResponse(e.getMessage)) }
      .get
  }
}
