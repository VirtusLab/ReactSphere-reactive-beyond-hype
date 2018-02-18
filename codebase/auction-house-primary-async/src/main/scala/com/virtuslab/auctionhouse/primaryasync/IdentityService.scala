package com.virtuslab.auctionhouse.primaryasync

import com.virtuslab.identity._

import scala.concurrent.Future

trait IdentityService {

  def createUser(request: CreateAccountRequest): Future[Unit]

  def signIn(request: SignInRequest): Future[String]

  def validateToken(token: String): Future[Option[String]]

  case class DuplicateUser(username: String) extends RuntimeException(username)

  case class FailedSignIn(username: String) extends RuntimeException(username)

}

trait IdentityServiceImpl extends IdentityService {

  def createUser(request: CreateAccountRequest): Future[Unit] = ???

  def signIn(request: SignInRequest): Future[String] = ???

  def validateToken(token: String): Future[Option[String]] = ???

}