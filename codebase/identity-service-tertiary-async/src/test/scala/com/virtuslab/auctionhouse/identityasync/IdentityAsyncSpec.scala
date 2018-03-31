package com.virtuslab.auctionhouse.identityasync

import akka.http.scaladsl.model.ContentTypes.`application/json`
import akka.http.scaladsl.model.HttpMethods.POST
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.datastax.driver.core.utils.UUIDs
import com.typesafe.scalalogging.Logger
import com.virtuslab.identity.{CreateAccountRequest, SignInRequest, TokenResponse, ValidateTokenRequest}
import com.virtuslab.{RequestMetrics, TraceId}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterEach, GivenWhenThen, Matchers, WordSpec}

import scala.concurrent.{ExecutionContext, Future}

class IdentityAsyncSpec extends WordSpec with Matchers with ScalaFutures with ScalatestRouteTest
  with GivenWhenThen with BeforeAndAfterEach
  with IdentityRoutes with TestIdentityServiceImpl
  with RequestMetrics {

  import spray.json._

  implicit val traceId: TraceId = TraceId(UUIDs.random().toString)

  protected def logger: Logger = Logger(getClass)

  protected def executionContext: ExecutionContext = system.dispatcher

  override def afterEach() = {
    clearData()
  }

  "Identity module" should {
    val sealedIdentityRoutes = Route.seal(identityRoutes)

    "create accounts" in {
      When("request to create account is sent to identity api")
      val body = CreateAccountRequest("testuser", "testpassword").toJson.prettyPrint
      val request = HttpRequest(uri = "/accounts", method = POST)
        .withEntity(HttpEntity(`application/json`, body))

      Then("response should have 201 status")
      request ~> sealedIdentityRoutes ~> check {
        status should ===(Created)
      }
    }

    "return error on duplicate account creation" in {
      Given("user testuser exists")
      addUser("testuser", "testpassword")

      When("request to create account with the same username is sent to identity api")
      val body = CreateAccountRequest("testuser", "testpassword").toJson.prettyPrint
      val request = HttpRequest(uri = "/accounts", method = POST)
        .withEntity(HttpEntity(`application/json`, body))

      Then("response should have 400 status and contain an error")
      request ~> sealedIdentityRoutes ~> check {
        status should ===(BadRequest)
        contentType should ===(`application/json`)
        entityAs[Error] should ===(Error("user 'testuser' already exists"))
      }
    }

    "sign in existing users" in {
      Given("user testuser exists and has password testpassword")
      addUser("testuser", "testpassword")

      When("request to sign in existing user with correct password is sent to identity api")
      val body = SignInRequest("testuser", "testpassword").toJson.prettyPrint
      val request = HttpRequest(uri = "/sign-in", method = POST)
        .withEntity(HttpEntity(`application/json`, body))

      Then("response should have 200 status containing token")
      request ~> sealedIdentityRoutes ~> check {
        status should ===(OK)
        contentType should ===(`application/json`)
        entityAs[TokenResponse] should ===(TokenResponse("f60e7614-0d56-4ef5-b6c8-3939d34d5ec2"))
      }
    }

    "reject sign in attempts when password is incorrect" in {
      Given("user testuser exists and has password testpassword")
      addUser("testuser", "testpassword")

      When("request to sign in existing user with incorrect password is sent to identity api")
      val invalidPasswordBody = SignInRequest("testuser", "invalid").toJson.prettyPrint
      val invalidPasswordRequest = HttpRequest(uri = "/sign-in", method = POST)
        .withEntity(HttpEntity(`application/json`, invalidPasswordBody))

      Then("response should have 401 status and contain an error")
      invalidPasswordRequest ~> sealedIdentityRoutes ~> check {
        status should ===(Unauthorized)
        contentType should ===(`application/json`)
        entityAs[Error] should ===(Error("wrong password or username"))
      }
    }

    "validate correct tokens" in {

      When("correct token for user is sent for validation")
      val validTokenBody = ValidateTokenRequest("valid_token")

    }

  }

  override def validateToken(token: String)(implicit traceId: TraceId): Future[Option[String]] = ???
}
