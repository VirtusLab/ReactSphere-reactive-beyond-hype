package com.virtuslab.auctionHouse.sync.signIn

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.{Date, UUID}

import com.datastax.driver.mapping.Mapper
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

  lazy val tokensMapper: Mapper[Token] = SessionManager.mapper(classOf[Token])
  lazy val accountsMapper: Mapper[Account] = SessionManager.mapper(classOf[Account])

  post("/") {
    val traceId = getTraceId
    val histogramTimer = requestsLatency.labels("signInRequest").startTimer()
    val signInReq = parsedBody.extract[SignInRequest]

    logger.info(s"[${traceId.id}] Received sign in request for user '${signInReq.username}' ...")

    try {
      accountsMapper.getOption(signInReq.username).map(u =>
        if (u.validatePassword(signInReq.password)) {
          val token = new Token(UUID.randomUUID().toString, u.username,
            new Date(Instant.now().plus(60, ChronoUnit.MINUTES).toEpochMilli))
          tokensMapper.save(token)
          logger.info(s"[${traceId.id}] Successful sign in for user '${signInReq.username}', responding with access token.")
          Ok(TokenResponse(token.bearer_token))
        } else {
          logger.warn(s"[${traceId.id}] Authentication failure for '${signInReq.username}' detected.")
          Unauthorized()
        }
      ).getOrElse {
        logger.warn(s"[${traceId.id}] Authentication failure for '${signInReq.username}' detected.")
        Unauthorized()
      }
    }
    catch {
      case exception: Throwable =>
        logger.error(s"[${traceId.id}] Error occured while signing in '${signInReq.username}':", exception)
        throw exception
    }
    finally {
      histogramTimer.observeDuration()
    }
  }
}
