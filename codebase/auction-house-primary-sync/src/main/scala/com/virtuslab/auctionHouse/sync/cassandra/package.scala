package com.virtuslab.auctionHouse.sync

import java.util.Date

import com.datastax.driver.mapping.annotations.{PartitionKey, Table}
import com.virtuslab.identity.CreateAccountRequest

import scala.annotation.meta.field

package object cassandra {

  import com.github.t3hnar.bcrypt._

  @Table(keyspace = "auction_house", name = "accounts")
  class Account(@(PartitionKey @field) val username: String, val password: String) {

    def this() {
      this(null, null)
    }
    def this(req: CreateAccountRequest) {
      this(req.username, req.password.bcrypt)
    }

    def validatePassword(decryptedPass: String): Boolean = decryptedPass isBcrypted password
  }


  @Table(keyspace = "auction_house", name = "tokens")
  class Token(@(PartitionKey @field) val token: String, val username: String, val expires_at: Date) {
    def this() {
      this(null, null, null)
    }
  }

}
