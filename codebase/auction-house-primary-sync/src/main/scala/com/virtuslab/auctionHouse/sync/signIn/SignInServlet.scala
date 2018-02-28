package com.virtuslab.auctionHouse.sync.signIn

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.{Date, UUID}

import com.virtuslab.auctionHouse.sync.BaseServlet
import com.virtuslab.auctionHouse.sync.cassandra.SessionManager.ScalaMapper
import com.virtuslab.auctionHouse.sync.cassandra.{Account, SessionManager, Token}
import com.virtuslab.identity.{SignInRequest, TokenResponse}
import org.json4s.{DefaultFormats, Formats}
import org.scalatra._

class SignInServlet extends BaseServlet {
  override protected implicit def jsonFormats: Formats = DefaultFormats

  before() {
    contentType = formats("json")
  }

  lazy val tokensMapper = SessionManager.mapper(classOf[Token])
  lazy val accountsMapper = SessionManager.mapper(classOf[Account])

  post("/") {
    val signInReq = parsedBody.extract[SignInRequest]
    accountsMapper.getOption(signInReq.username).map(u =>
      if (u.validatePassword(signInReq.password)) {
        val token = new Token(UUID.randomUUID().toString, u.username,
          new Date(Instant.now().plus(60, ChronoUnit.MINUTES).toEpochMilli))
        tokensMapper.save(token)
        Ok(TokenResponse(token.token))
      } else {
        Unauthorized()
      }
    ).getOrElse {
      Unauthorized()
    }
  }
}
