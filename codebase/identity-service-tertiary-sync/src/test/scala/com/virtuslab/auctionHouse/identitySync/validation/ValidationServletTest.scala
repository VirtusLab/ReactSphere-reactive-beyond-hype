package com.virtuslab.auctionHouse.identitySync.validation

import java.time.Instant
import java.util.Date

import com.datastax.driver.mapping.Mapper
import com.virtuslab.auctionHouse.identitySync.cassandra.Token
import com.virtuslab.base.sync.BaseServletTest
import org.mockito.Mockito._

class ValidationServletTest extends BaseServletTest(classOf[TestableValidationServlet]) {

  "Validation servlet" should {
    "return unauthorized" when {
      "user provide invalid token" in {
        post("/", """{"token": "invalid"}""", jsonHeader) {
          status should equal(401)
        }
      }
    }

    "return ok" when {
      "user provide valid token" in {
        post("/", """{"token": "valid"}""", jsonHeader) {
          status should equal(200)
          body should equal("""{"username":"u1"}""")
        }
      }
    }
  }
}

class TestableValidationServlet extends ValidationServlet {

  private val mapperMock = mock(classOf[Mapper[Token]])
  when(mapperMock.get("invalid")).thenReturn(null)
  when(mapperMock.get("valid")).thenReturn(new Token("valid", "u1", Date.from(Instant.now().plusSeconds(60))))

  override lazy val validationService = new ValidationService {
    override lazy val tokensMapper: Mapper[Token] = mapperMock
  }
}