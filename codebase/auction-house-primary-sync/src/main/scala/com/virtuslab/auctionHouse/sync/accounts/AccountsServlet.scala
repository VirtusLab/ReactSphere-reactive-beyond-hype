package com.virtuslab.auctionHouse.sync.accounts

import com.virtuslab.auctionHouse.sync.cassandra.{Account, CassandraSession}
import com.virtuslab.auctionHouse.sync.commons.ServletModels.ErrorResponse
import com.virtuslab.identity.CreateAccountRequest
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.json.JacksonJsonSupport
import org.scalatra.{BadRequest, Created, ScalatraServlet}

class AccountsServlet extends ScalatraServlet with JacksonJsonSupport {
  override protected implicit def jsonFormats: Formats = DefaultFormats

  val accountMapper = CassandraSession.mappingManager.mapper(classOf[Account])

  post("/") {
    val accountRequest = parsedBody.extract[CreateAccountRequest]
    Option(accountMapper.get(accountRequest.username)).map { u =>
      BadRequest(ErrorResponse(s"Account ${u.username} already exists"))
    }.getOrElse {
      accountMapper.save(new Account(accountRequest))
      Created()
    }
  }
}
