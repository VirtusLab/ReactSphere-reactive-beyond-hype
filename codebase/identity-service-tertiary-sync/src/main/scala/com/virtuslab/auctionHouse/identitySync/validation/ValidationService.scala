package com.virtuslab.auctionHouse.identitySync.validation

import java.util.Date

import com.datastax.driver.mapping.Mapper
import com.virtuslab.auctionHouse.identitySync.cassandra.SessionManager.ScalaMapper
import com.virtuslab.auctionHouse.identitySync.cassandra.{SessionManager, Token}
import com.virtuslab.identity.ValidateTokenRequest

class ValidationService {

  lazy val tokensMapper: Mapper[Token] = SessionManager.mapper(classOf[Token])

  def validateToken(req: ValidateTokenRequest): Option[String] = {
    tokensMapper.getOption(req.token)
      .filter(_.expires_at.compareTo(new Date()) > 0)
      .map(_.username)
  }

}

