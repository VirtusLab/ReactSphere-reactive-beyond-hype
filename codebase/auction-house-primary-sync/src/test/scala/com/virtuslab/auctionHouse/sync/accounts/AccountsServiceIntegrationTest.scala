package com.virtuslab.auctionHouse.sync.accounts

import com.datastax.driver.mapping.Mapper
import com.virtuslab.auctionHouse.sync.cassandra.SessionManager.ScalaMapper
import com.virtuslab.auctionHouse.sync.cassandra.{Account, SessionManager}
import com.virtuslab.auctionhouse.cassandra.CassandraIntegrationTest
import com.virtuslab.identity.CreateAccountRequest
import org.scalatest.{Matchers, WordSpec}

class AccountsServiceIntegrationTest extends WordSpec with CassandraIntegrationTest with Matchers {

  val sessionManager = new SessionManager {
    override lazy val session = getSession
  }

  val accountsService = new AccountService {
    override lazy val accountMapper: Mapper[Account] = sessionManager.mapper(classOf[Account], keyspace)
  }

  "Accounts service" should {
    "create account" when {
      "user provide correct credentials" in {
        accountsService.createAccount(CreateAccountRequest("u1", "p1"))
        accountsService.accountMapper.getOption("u1").isDefined should equal(true)
      }
    }

  }
}
