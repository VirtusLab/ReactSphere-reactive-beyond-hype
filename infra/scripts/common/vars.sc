object StackType {
  def fromString(name: String): StackType = name.trim.toLowerCase match {
    case "sync" => SyncStack
    case "async" => AsyncStack
    case _ => throw new IllegalArgumentException(s"Unrecognized stack type: ${name}")
  }
}

sealed trait StackType {
  def paradigm: String
}

case object SyncStack extends StackType {
  override val paradigm = "sync"
}
case object AsyncStack extends StackType {
  override val paradigm = "async"
}

val AUCTION_APP = "auction-house-primary"
val AUCTION_PORT = 8080

val BILLING_APP = "billing-service-secondary"
val BILLING_PORT = 8090

val IDENTITY_APP = "identity-service-tertiary"
val IDENTITY_PORT = 8100

val PAYMENT_PORT = 9000
val PAYMENT_SYSTEM = "payment-system"

val CASSANDRA_HOST = "cassandra"
val CASSANDRA_PORT = 9042

val GATLING_DELAY = 60

def apps = Seq(
  AUCTION_APP -> AUCTION_PORT,
  BILLING_APP -> BILLING_PORT,
  IDENTITY_APP -> IDENTITY_PORT
)

def backingServices = Seq(
  PAYMENT_SYSTEM -> PAYMENT_PORT
)

val gatling = "gatling-tests"

val AWS_KEY_K8S_PROP = "key"
val K8S_AWS_NAME = "aws"
val AWS_SECRET_K8S_PROP = "secret"

val awsKey = Option(System.getenv("BILLING_WORKER_AWS_KEY")).getOrElse("AWS_ACCESS_KEY_NOT_SET")
val awsSecret = Option(System.getenv("BILLING_WORKER_AWS_SECRET")).getOrElse("AWS_SECRET_NOT_SET")


case class PublishOptions(sbtTask: PublishTask, registry: Registry)

sealed trait PublishTask {
  def name: String
}

case object PublishLocal extends PublishTask {
  override def name: String = "publishLocal"
}
case object Publish extends PublishTask {
  override def name: String = "publish"
}

sealed trait Environment {
  def name: String
}

case object Dev extends Environment {
  def name: String = "dev"
}

case object Prod extends Environment {
  def name: String = "prod"
}

sealed trait Registry {
  def value: String
}

case object Local extends Registry {
  def value: String = "docker-registry.local"
}

case object Quay extends Registry {
  def value: String = "quay.io/virtuslab"
}

case class StepDefinitions(tests: Boolean = true, publish: Boolean = true, gatling: Boolean = false)