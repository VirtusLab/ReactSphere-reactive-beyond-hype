package com.virtuslab.auctionhouse.primaryasync

import com.virtuslab.TraceId
import com.virtuslab.identity.{CreateAccountRequest, SignInRequest, User}

import scala.concurrent.Future
import scala.concurrent.Future.{failed, successful}

trait TestIdentityServiceImpl extends IdentityService {

  private val CorrectTestToken = "f60e7614-0d56-4ef5-b6c8-3939d34d5ec2"
  private var userMap = Map.empty[String, User]

  def addUser(username: String, password: String): Unit = {
    val user = CreateAccountRequest(username, password).createUser
    userMap = userMap + (username -> user)
  }

  def clearData(): Unit = {
    userMap = Map.empty
  }

  def createUser(request: CreateAccountRequest)(implicit traceId: TraceId): Future[Unit] = {
    if (userMap contains request.username) failed(DuplicateUser(request.username))
    else {
      val user = request.createUser
      userMap = userMap + (user.username -> user)
      successful(())
    }
  }

  def signIn(request: SignInRequest)(implicit traceId: TraceId): Future[String] =
    userMap.get(request.username)
      .map { user => user.validatePassword(request.password) }
      .flatMap { passwordCorrect => if (passwordCorrect) Some(CorrectTestToken) else None }
      .fold[Future[String]](failed(FailedSignIn(request.username)))(successful)

  def validateToken(token: String): Future[Option[String]] =
    if (token == "valid-token") successful(Some("testuser"))
    else successful(None)
}
