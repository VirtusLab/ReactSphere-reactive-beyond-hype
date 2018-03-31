package com.virtuslab

trait HeadersSupport {

  protected val AUTHORIZATION_KEYS = Seq(
    "Authorization",
    "HTTP_AUTHORIZATION",
    "X-HTTP_AUTHORIZATION",
    "X_HTTP_AUTHORIZATION"
  )

  type Headers = Seq[(String, String)]

  def traceHeaders(implicit traceId: TraceId): Headers =
    ("X-Trace-Id", traceId.id) :: ("Content-Type", "application/json") :: Nil

  def authHeaders(implicit maybeAuthToken: Option[AuthToken]): Headers = maybeAuthToken match {
    case Some(authToken) => (AUTHORIZATION_KEYS.head, s"bearer ${authToken.value}") :: Nil
    case None => Nil
  }
}
