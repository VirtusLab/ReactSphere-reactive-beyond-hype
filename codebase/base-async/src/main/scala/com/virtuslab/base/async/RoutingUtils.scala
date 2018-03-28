package com.virtuslab.base.async

import spray.json.{DefaultJsonProtocol, RootJsonFormat}

trait RoutingUtils extends DefaultJsonProtocol {

  case class Error(error: String)

  implicit lazy val errorFormat: RootJsonFormat[Error] = jsonFormat1(Error)

}
