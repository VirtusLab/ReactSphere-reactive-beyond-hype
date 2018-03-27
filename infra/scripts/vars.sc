def env: String = sys.env.getOrElse("ENV", "dev")

def paradigm: String = sys.env.getOrElse("PARADIGM", "async")

def apps = Seq(
  "auction-house-primary"
)