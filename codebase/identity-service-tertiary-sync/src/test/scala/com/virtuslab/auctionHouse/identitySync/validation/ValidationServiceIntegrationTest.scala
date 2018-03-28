package com.virtuslab.auctionHouse.identitySync.validation

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.{Date, UUID}

import com.datastax.driver.mapping.Mapper
import com.virtuslab.auctionHouse.identitySync.cassandra.{SessionManager, Token}
import com.virtuslab.auctionhouse.cassandra.CassandraIntegrationTest
import com.virtuslab.identity.ValidateTokenRequest
import org.scalatest.{Matchers, WordSpec}

class ValidationServiceIntegrationTest extends WordSpec with CassandraIntegrationTest with Matchers {

  val sessionManager = new SessionManager {
    override lazy val session = getSession
  }

  val validationService = new ValidationService {
    override lazy val tokensMapper: Mapper[Token] = sessionManager.mapper(classOf[Token], keyspace)
  }

  "Validation service" should {
    "fetch username bound to token" when {
      "user provide correct authentication token" in {
        val tokenValue = UUID.randomUUID().toString
        val token = new Token(tokenValue, "u1",
          Date.from(Instant.now().plus(60, ChronoUnit.MINUTES))
        )
        validationService.tokensMapper.save(token)

        val result = validationService.validateToken(ValidateTokenRequest(tokenValue))
        result.isDefined shouldBe true
        result.get shouldBe "u1"
      }
    }

    "return no result" when {
      "user provide invalid authentication token" in {
        val result = validationService.validateToken(ValidateTokenRequest("invalid"))
        result.isDefined shouldBe false
      }
    }

  }
}
