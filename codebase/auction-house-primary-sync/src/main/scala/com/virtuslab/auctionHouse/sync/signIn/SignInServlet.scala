package com.virtuslab.auctionHouse.sync.signIn

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.{Date, UUID}

import com.virtuslab.auctionHouse.sync.cassandra.{Account, CassandraSession, Token}
import com.virtuslab.identity.{SignInRequest, TokenResponse}
import org.json4s.{DefaultFormats, Formats}
import org.scalatra._
import org.scalatra.json.JacksonJsonSupport

class SignInServlet extends ScalatraServlet with JacksonJsonSupport {
  override protected implicit def jsonFormats: Formats = DefaultFormats

  lazy val tokensMapper = CassandraSession.mappingManager.mapper(classOf[Token])
  lazy val accountsMapper = CassandraSession.mappingManager.mapper(classOf[Account])

  post("/") {
    val signInReq = parsedBody.extract[SignInRequest]
    Option(accountsMapper.get(signInReq.username)).map(u =>
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
