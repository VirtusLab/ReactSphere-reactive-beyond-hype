package com.virtuslab.helloworldsync

import org.scalatest.GivenWhenThen
import org.scalatra.test.scalatest._

class SynchronousHelloWorldSpec extends ScalatraWordSpec with GivenWhenThen {

  addServlet(classOf[HelloWorldServlet], "/*")

  "HelloWorld" should {
    "return json with Hello World! message" in {
      When("request is being sent to HelloWorld api")
      get("/") {
        Then("response should contain valid HelloWorld json message")
        status should equal(200)
        response.getHeader("Content-Type") should equal("application/json;charset=utf-8")
        body should equal ("""{"message":"Hello World!"}""")
      }
    }
  }

}
