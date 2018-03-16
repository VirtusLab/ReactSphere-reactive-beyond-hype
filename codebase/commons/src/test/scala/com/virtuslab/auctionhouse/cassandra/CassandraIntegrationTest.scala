package com.virtuslab.auctionhouse.cassandra

import com.datastax.driver.core.querybuilder.QueryBuilder
import com.datastax.driver.core.{ResultSet, Session}
import com.virtuslab.cassandra.CassandraClient
import org.apache.log4j.{Level, Logger}
import org.cassandraunit.CassandraCQLUnit
import org.cassandraunit.dataset.cql.ClassPathCQLDataSet
import org.junit.runner.Description
import org.junit.runners.model.Statement
import org.scalatest.{Outcome, TestSuite, TestSuiteMixin}

import scala.concurrent.Future

trait CassandraIntegrationTest extends TestSuiteMixin with CassandraClient {
  this: TestSuite =>

  Logger.getLogger("Sigar").setLevel(Level.OFF)

  val keyspace = "microservices"

  import com.virtuslab.AsyncUtils.Implicits._

  private val cassandraCQLUnit = new CassandraCQLUnit(new ClassPathCQLDataSet("schema.cql", keyspace))

  abstract override def withFixture(test: NoArgTest): Outcome = {
    var outcome: Outcome = null

    val statementBody = () => outcome = super.withFixture(test)

    cassandraCQLUnit(
      new Statement() {
        override def evaluate(): Unit = statementBody()
      },
      Description.createSuiteDescription("JUnit rule wrapper")
    ).evaluate()

    outcome
  }

  override def getSession: Session = cassandraCQLUnit.session

  override def getSessionAsync: Future[Session] = cassandraCQLUnit.getCluster.connectAsync().asScala

  def fetch(table: String): ResultSet = getSession.execute(QueryBuilder.select().all().from(keyspace, table))

}