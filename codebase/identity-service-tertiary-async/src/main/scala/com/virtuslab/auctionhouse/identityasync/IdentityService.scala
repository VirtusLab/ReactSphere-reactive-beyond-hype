package com.virtuslab.auctionhouse.identityasync

import java.sql.Timestamp
import java.time.LocalDateTime
import java.util.{Date, UUID}

import com.datastax.driver.core.querybuilder.QueryBuilder.{insertInto, select, eq => equal}
import com.datastax.driver.core.{ResultSet, Session}
import com.typesafe.scalalogging.Logger
import com.virtuslab.cassandra.CassandraClient
import com.virtuslab.identity._
import com.virtuslab.{CassandraQueriesMetrics, Logging, RequestMetrics, TraceId}

import scala.concurrent.{ExecutionContext, Future}

trait IdentityService {

  def createUser(request: CreateAccountRequest)(implicit traceId: TraceId): Future[Unit]

  def signIn(request: SignInRequest)(implicit traceId: TraceId): Future[String]

  def validateToken(token: String)(implicit traceId: TraceId): Future[Option[String]]

  case class DuplicateUser(username: String) extends RuntimeException(username)

  case class FailedSignIn(username: String) extends RuntimeException(username)

}

trait IdentityServiceImpl extends IdentityService {
  this: CassandraClient with CassandraQueriesMetrics with RequestMetrics =>


  import com.virtuslab.AsyncUtils.Implicits._

  protected implicit def executionContext: ExecutionContext

  protected def log: Logger

  private lazy val sessionFuture: Future[Session] = getSessionAsync

  def createUser(request: CreateAccountRequest)(implicit traceId: TraceId): Future[Unit] = {
    val user = request.createUser

    val query = insertInto("microservices", "accounts")
      .value("username", user.username)
      .value("password", user.passwordHash)
      .ifNotExists()


    cassandraTimingAsync(1, "create_account") {
      for {
        session <- sessionFuture
        rs <- session.executeAsync(query).asScala
      } yield {
        if (!rs.one.getBool("[applied]")) throw DuplicateUser(user.username)
      }
    }
  }

  def signIn(request: SignInRequest)(implicit traceId: TraceId): Future[String] = {

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
      passwordHashRs <- cassandraTimingAsync(1, "sign_in") { session.executeAsync(fetchUserQuery).asScala }
      user <- userOrMissingUser(request.username, passwordHashRs)
      _ <- cassandraTimingAsync(1, "save_token") { session.executeAsync(insertTokenQuery).asScala }
    } yield {
      if (user validatePassword request.password) token
      else throw FailedSignIn(request.username)
    }
  }

  def validateToken(token: String)(implicit traceId: TraceId): Future[Option[String]] = {
    val validateTokenQuery = select().all()
      .from("microservices", "tokens")
      .where(equal("bearer_token", token))

    cassandraTimingAsync(1, "validate_token") {
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

}