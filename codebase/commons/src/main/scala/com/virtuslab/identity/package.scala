package com.virtuslab

package object identity {

  import com.github.t3hnar.bcrypt._

  case class CreateAccountRequest(username: String, password: String) {
    def createUser: User = User(username, password.bcrypt)
  }

  case class SignInRequest(username: String, password: String)

  case class User(username: String, passwordHash: String) {
    def validatePassword(password: String): Boolean = password isBcrypted passwordHash
  }

  case class Token(token: String)

}
