package com.virtuslab.auctionhouse.primaryasync

import akka.http.scaladsl.model.ContentTypes.`application/json`
import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.virtuslab.identity.{CreateAccountRequest, SignInRequest}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{GivenWhenThen, Matchers, WordSpec}

class AuctionHouseAsyncSpec extends WordSpec with Matchers with ScalaFutures
  with ScalatestRouteTest with GivenWhenThen with Routes with TestIdentityServiceImpl {

  import spray.json._

  "Identity module" should {
    "create accounts" in {
      When("request to create account is sent to identity api")
      val body = CreateAccountRequest("testuser", "testpassword").toJson.prettyPrint
      val request = HttpRequest(uri = "/api/v1/accounts", method = HttpMethods.POST)
        .withEntity(HttpEntity(`application/json`, body))

      Then("response should have 201 status")
      request ~> routes ~> check {
        status should ===(StatusCodes.Created)
      }

      clearData()
    }

    "return error on duplicate account creation" in {
      Given("user testuser exists")
      addUser("testuser", "testpassword")

      When("request to create account with the same username is sent to identity api")
      val body = CreateAccountRequest("testuser", "testpassword").toJson.prettyPrint
      val request = HttpRequest(uri = "/api/v1/accounts", method = HttpMethods.POST)
        .withEntity(HttpEntity(`application/json`, body))

      Then("response should have 400 status")
      request ~> routes ~> check {
        status should ===(StatusCodes.BadRequest)
        contentType should ===(ContentTypes.`application/json`)
        entityAs[String] should ===("""{"error":"user testuser already exists"}""")
      }

      clearData()
    }

    "sign in existing users" in {
      Given("user testuser exists and has password testpassword")
      addUser("testuser", "testpassword")

      When("request to sign in existing user with correct password is sent to identity api")
      val body = SignInRequest("testuser", "testpassword").toJson.prettyPrint
      val request = HttpRequest(uri = "/api/v1/sign-in", method = HttpMethods.POST)
        .withEntity(HttpEntity(`application/json`, body))

      Then("response should have 200 status containing token")
      request ~> routes ~> check {
        status should ===(StatusCodes.OK)
        contentType should ===(ContentTypes.`application/json`)
        entityAs[String] should ===("""{"token":"f60e7614-0d56-4ef5-b6c8-3939d34d5ec2"}""")
      }

      clearData()
    }
  }

}
