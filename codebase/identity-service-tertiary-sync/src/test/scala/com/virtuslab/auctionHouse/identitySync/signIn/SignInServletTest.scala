package com.virtuslab.auctionHouse.identitySync.signIn

import com.datastax.driver.mapping.Mapper
import com.virtuslab.auctionHouse.identitySync.cassandra.{Account, Token}
import com.virtuslab.base.sync.BaseServletTest
import com.virtuslab.identity.TokenResponse
import org.mockito.Mockito.{mock, when}
import org.json4s._
import org.json4s.jackson.JsonMethods._

class SignInServletTest extends BaseServletTest(classOf[TestableSignInServlet]) {

  "Signing in" should {
    "return unauthorized" when {
      "user not exists" in {
        post("/", """{"username": "u1", "password" : "p1"}""", jsonHeader) {
          status should equal(401)
        }
      }

      "user passed wrong password" in {
        post("/", """{"username": "u2", "password" : "p1"}""", jsonHeader) {
          status should equal(401)
        }
      }
    }

    "return token" when {
      "user provides correct credentials" in {
        post("/", """{"username": "u2", "password" : "p2"}""", jsonHeader) {
          status should equal(200)
          val token = parse(body).extract[TokenResponse]
          token.token.trim.isEmpty shouldNot equal(true)
        }
      }
    }
  }
}

class TestableSignInServlet extends SignInServlet {

  import com.github.t3hnar.bcrypt._

  private val accountsMapperMock = mock(classOf[Mapper[Account]])
  when(accountsMapperMock.get("u1")).thenReturn(null)
  when(accountsMapperMock.get("u2")).thenReturn(new Account("u2", "p2".bcrypt))

  private val tokensMapperMock = mock(classOf[Mapper[Token]])

  override lazy val tokensMapper: Mapper[Token] = tokensMapperMock
  override lazy val accountsMapper: Mapper[Account] = accountsMapperMock
}
