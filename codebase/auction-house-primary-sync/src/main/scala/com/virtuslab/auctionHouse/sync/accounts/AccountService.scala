package com.virtuslab.auctionHouse.sync.accounts

import com.virtuslab.auctionHouse.sync.accounts.AccountService.DuplicatedEntityException
import com.virtuslab.auctionHouse.sync.cassandra.SessionManager.ScalaMapper
import com.virtuslab.auctionHouse.sync.cassandra.{Account, SessionManager}
import com.virtuslab.identity.CreateAccountRequest

class AccountService {

  lazy val accountMapper = SessionManager.mapper(classOf[Account])

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
