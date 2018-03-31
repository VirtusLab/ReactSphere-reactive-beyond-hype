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

def apps = Seq(
  AUCTION_APP -> AUCTION_PORT,
  BILLING_APP -> BILLING_PORT,
  IDENTITY_APP -> IDENTITY_PORT
)

def backingServices = Seq(
  PAYMENT_SYSTEM -> PAYMENT_PORT
)