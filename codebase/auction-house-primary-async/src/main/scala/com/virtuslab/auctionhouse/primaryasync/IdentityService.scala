package com.virtuslab.auctionhouse.primaryasync

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import com.virtuslab.identity._

import scala.concurrent.Future

trait IdentityService extends SprayJsonSupport {

  case class DuplicateUser(username: String) extends RuntimeException(username)
  case class FailedSignIn(username: String) extends RuntimeException(username)

  def createUser(request: CreateAccountRequest): Future[Unit]

  def signIn(request: SignInRequest): Future[String]

}

trait IdentityServiceImpl extends IdentityService {

  def createUser(request: CreateAccountRequest): Future[Unit] = ???

  def signIn(request: SignInRequest): Future[String] = ???

}