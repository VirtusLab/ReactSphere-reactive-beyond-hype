package com.lightbend.akka.http.sample

import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.typesafe.scalalogging.Logger
import com.virtuslab.helloworldasync.Routes
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{GivenWhenThen, Matchers, WordSpec}

class ReactiveHelloWorldSpec extends WordSpec with Matchers with ScalaFutures
  with ScalatestRouteTest with GivenWhenThen with Routes {

  "HelloWorld" should {
    "return json with Hello World! message" in {
      When("request is being sent to HelloWorld api")
      val request = HttpRequest(uri = "/")

      Then("response should contain valid HelloWorld json message")
      request ~> routes ~> check {
        status should ===(StatusCodes.OK)

        contentType should ===(ContentTypes.`application/json`)

        entityAs[String] should ===("""{"message":"Hello World!"}""")
      }
    }
  }

  override protected def logger: Logger = Logger(classOf[ReactiveHelloWorldSpec])
}
