package com.virtuslab.auctionhouse.primaryasync

import java.sql.Timestamp
import java.time.LocalDateTime
import java.util.{Date, UUID}

import com.datastax.driver.core.{ResultSet, Session}
import com.datastax.driver.core.querybuilder.QueryBuilder
import com.datastax.driver.core.querybuilder.QueryBuilder.{insertInto, select, eq => equal}
import com.virtuslab.cassandra.CassandraClient
import com.virtuslab.identity._

import scala.concurrent.{ExecutionContext, Future}

trait IdentityService {

  def createUser(request: CreateAccountRequest): Future[Unit]

  def signIn(request: SignInRequest): Future[String]

  def validateToken(token: String): Future[Option[String]]

  case class DuplicateUser(username: String) extends RuntimeException(username)

  case class FailedSignIn(username: String) extends RuntimeException(username)

}

trait IdentityServiceImpl extends IdentityService {
  this: CassandraClient =>

  import com.virtuslab.AsyncUtils.Implicits._

  implicit def executionContext: ExecutionContext

  private lazy val sessionFuture: Future[Session] = getSessionAsync

  def createUser(request: CreateAccountRequest): Future[Unit] = {
    val user = request.createUser

    val query = insertInto("microservices", "accounts")
      .value("username", user.username)
      .value("password", user.passwordHash)
      .ifNotExists()

    for {
      session <- sessionFuture
      rs <- session.executeAsync(query).asScala
    } yield {
      if (!rs.one.getBool("[applied]")) throw DuplicateUser(user.username)
    }
  }

  def signIn(request: SignInRequest): Future[String] = {

    def userOrMissingUser(username: String, passwordHashRs: ResultSet): Future[User] = Future {
      val maybeRow = Option(passwordHashRs.one())
      val passwordHash = maybeRow.map(_.get("password", classOf[String])).getOrElse {
        throw FailedSignIn(username)
      }

      User(username, passwordHash)
    }

    val fetchUserQuery = select("password")
      .from("microservices", "accounts")
      .where(equal("username", request.username))

    val token = UUID.randomUUID().toString
    val timestamp = Timestamp.valueOf(LocalDateTime.now().plusHours(1L))

    val insertTokenQuery = insertInto("microservices", "tokens")
      .value("bearer_token", token)
      .value("username", request.username)
      .value("expires_at", timestamp)

    for {
      session <- sessionFuture
      passwordHashRs <- session.executeAsync(fetchUserQuery).asScala
      user <- userOrMissingUser(request.username, passwordHashRs)
      _ <- session.executeAsync(insertTokenQuery).asScala
    } yield {
      if (user validatePassword request.password) token
      else throw FailedSignIn(request.username)
    }
  }

  def validateToken(token: String): Future[Option[String]] = {
    val validateTokenQuery = select().all()
        .from("microservices", "tokens")
        .where(equal("bearer_token", token))

    for {
      session <- sessionFuture
      tokenRs <- session.executeAsync(validateTokenQuery).asScala
    } yield {
      val now = new Date()
      Option(tokenRs.one())
        .filter(_.getTimestamp("expires_at") after now)
        .map(_.getString("username"))
    }

  }

}