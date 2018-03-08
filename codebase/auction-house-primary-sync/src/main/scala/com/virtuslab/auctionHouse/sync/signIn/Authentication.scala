package com.virtuslab.auctionHouse.sync.signIn

import java.util.Date

import com.datastax.driver.mapping.Mapper
import com.virtuslab.auctionHouse.sync.cassandra.{Account, SessionManager, Token}
import org.scalatra.ScalatraBase
import com.virtuslab.auctionHouse.sync.cassandra.SessionManager.ScalaMapper

trait Authentication extends ScalatraBase {

  lazy val tokensMapper: Mapper[Token] = SessionManager.mapper(classOf[Token])
  lazy val accountsMapper: Mapper[Account] = SessionManager.mapper(classOf[Account])

  private val AUTHORIZATION_KEYS = Seq("Authorization", "HTTP_AUTHORIZATION", "X-HTTP_AUTHORIZATION",
    "X_HTTP_AUTHORIZATION")

  def auth[T](fun: Account => T): T = {
    val account = getToken.flatMap { requestedToken =>
      tokensMapper.getOption(requestedToken)
        .filter(_.expires_at.compareTo(new Date()) > 0)
        .flatMap(t => accountsMapper.getOption(t.username))
    }.getOrElse(halt(status = 401))
    fun(account)
  }

  private def getToken: Option[String] = {
    AUTHORIZATION_KEYS.find(k => Option(request.getHeader(k)).isDefined)
      .map(request.getHeader(_).trim.split(" ", 2).map(_.trim))
      .find(arr => arr.length == 2 && arr(0).equalsIgnoreCase("bearer"))
      .map(_(1))
  }
}
