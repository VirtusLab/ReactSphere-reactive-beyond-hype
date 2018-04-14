package com.virtuslab.auctionHouse.identitySync.accounts

import com.datastax.driver.mapping.Mapper
import com.virtuslab.CassandraQueriesMetrics
import com.virtuslab.auctionHouse.identitySync.accounts.AccountService.DuplicatedEntityException
import com.virtuslab.auctionHouse.identitySync.cassandra.SessionManager.ScalaMapper
import com.virtuslab.auctionHouse.identitySync.cassandra.{Account, SessionManager}
import com.virtuslab.identity.CreateAccountRequest

class AccountService extends CassandraQueriesMetrics {

  lazy val accountMapper: Mapper[Account] = SessionManager.mapper(classOf[Account])

  def createAccount(req: CreateAccountRequest): Unit = {
    usingCassandra(1) {
      accountMapper.getOption(req.username).map { u =>
        throw new DuplicatedEntityException(s"Account ${u.username} already exists")
      }
    }.getOrElse {
      usingCassandra(1) {
        accountMapper.save(new Account(req))
      }
    }
  }
}

object AccountService {

  class DuplicatedEntityException(msg: String) extends RuntimeException(msg)

}
