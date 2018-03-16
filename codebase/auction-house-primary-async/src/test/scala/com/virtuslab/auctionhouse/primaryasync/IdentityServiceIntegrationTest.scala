package com.virtuslab.auctionhouse.primaryasync

import com.datastax.driver.core.utils.UUIDs
import com.typesafe.scalalogging.Logger
import com.virtuslab.TraceId
import com.virtuslab.auctionhouse.cassandra.CassandraIntegrationTest
import com.virtuslab.identity.{CreateAccountRequest, SignInRequest}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{GivenWhenThen, Matchers, OptionValues, WordSpec}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}

class IdentityServiceIntegrationTest extends WordSpec with Matchers with ScalaFutures with GivenWhenThen
  with OptionValues with CassandraIntegrationTest with IdentityServiceImpl {

  implicit val executionContext: ExecutionContext = global
  implicit val traceId: TraceId = TraceId(UUIDs.random().toString)

  override def logger: Logger = Logger(getClass)

  "Identity service" should {

    "create accounts" in {
      When("user account is created")
      Await.result(createUser(CreateAccountRequest("testuser", "testpassword")), 1.second)

      Then("account should exist in database")
      val userRow = fetch("accounts").one()
      userRow.get("username", classOf[String]) should be("testuser")
    }

    "fail when creating duplicate accounts" in {
      Given("user testuser exists")
      Await.result(createUser(CreateAccountRequest("testuser", "testpassword")), 1.second)

      When("duplicate account is created")
      val duplicateFut = createUser(CreateAccountRequest("testuser", "anotherpasswd"))

      Then("duplicate user error should occur")
      intercept[DuplicateUser] {
        Await.result(duplicateFut, 1.second)
      }
    }

    "fail to sign in users when given incorrect password" in {
      Given("user testuser exists")
      Await.result(createUser(CreateAccountRequest("testuser", "testpassword")), 1.second)

      When("user tries to sign in using incorrect password")
      val signInFut = signIn(SignInRequest("testuser", "incorrect"))

      Then("failed sign in error should occur")
      intercept[FailedSignIn] {
        Await.result(signInFut, 1.second)
      }
    }

    "fail to sign in non-existing users" in {
      Given("no users exist")
      When("non-existing user tries to sign in")
      val signInFut = signIn(SignInRequest("doesntexist", "doesntmatter"))

      Then("failed sign in error should occur")
      intercept[FailedSignIn] {
        Await.result(signInFut, 1.second)
      }
    }

    "fail when invalid authentication token is submitted" in {
      When("invalid token is submitted")
      val validateTokenFut = validateToken("invalid")

      Then("no username should be returned")
      val validationResult = Await.result(validateTokenFut, 1.second)
      validationResult should be(None)
    }

    "sign in users with correct passwords" in {
      Given("user testuser exists")
      Await.result(createUser(CreateAccountRequest("testuser", "testpassword")), 1.second)

      When("user tries to sign in using correct password")
      val signInFut = signIn(SignInRequest("testuser", "testpassword"))

      Then("authentication token should be returned and no error should occur")
      val token = Await.result(signInFut, 1.second)
      val maybeUsername = Await.result(validateToken(token), 1.second)

      maybeUsername should contain("testuser")
    }

  }

}
