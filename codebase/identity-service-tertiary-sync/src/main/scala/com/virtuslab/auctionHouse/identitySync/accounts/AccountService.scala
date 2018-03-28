package com.virtuslab.auctionHouse.identitySync.accounts

import com.datastax.driver.mapping.Mapper
import com.virtuslab.auctionHouse.identitySync.accounts.AccountService.DuplicatedEntityException
import com.virtuslab.auctionHouse.identitySync.cassandra.SessionManager.ScalaMapper
import com.virtuslab.auctionHouse.identitySync.cassandra.{Account, SessionManager}
import com.virtuslab.identity.CreateAccountRequest

class AccountService {

  lazy val accountMapper: Mapper[Account] = SessionManager.mapper(classOf[Account])

  def createAccount(req: CreateAccountRequest): Unit = {
    accountMapper.getOption(req.username).map { u =>
      throw new DuplicatedEntityException(s"Account ${u.username} already exists")
    }.getOrElse {
      accountMapper.save(new Account(req))
    }
  }
}
object AccountService {
  class DuplicatedEntityException(msg: String) extends RuntimeException(msg)
}
