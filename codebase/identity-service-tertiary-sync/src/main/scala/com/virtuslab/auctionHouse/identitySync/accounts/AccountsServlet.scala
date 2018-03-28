package com.virtuslab.auctionHouse.identitySync.accounts

import com.virtuslab.auctionHouse.identitySync.accounts.AccountService.DuplicatedEntityException
import com.virtuslab.auctionHouse.identitySync.commons.ServletModels.ErrorResponse
import com.virtuslab.base.sync.BaseServlet
import com.virtuslab.identity.CreateAccountRequest
import org.scalatra.{BadRequest, Created}

import scala.util.Try

class AccountsServlet extends BaseServlet {

  override def servletName: String = "Accounts"

  lazy val accountService = new AccountService()

  post("/") {
    val traceId = getTraceId
    val histogramTimer = requestsLatency.labels("createAccountRequest").startTimer()
    val accountRequest = parsedBody.extract[CreateAccountRequest]

    logger.info(s"[${traceId.id}] Received account creation request for user '${accountRequest.username}' ...")

    val attempt = Try(accountService.createAccount(accountRequest))
      .map(_ => {
        logger.info(s"[${traceId.id}] Account for '${accountRequest.username}' created.")
        Created()
      })
      .recover { case e: DuplicatedEntityException =>
        logger.warn(s"[${traceId.id}] Account for '${accountRequest.username}' already exists.")
        BadRequest(ErrorResponse(e.getMessage))
      }

    if (attempt.isFailure) {
      logger.error(s"[${traceId.id}] Error occured while creating account for user '${accountRequest.username}':", attempt.failed.get)
    }

    histogramTimer.observeDuration()

    attempt.get
  }
}
