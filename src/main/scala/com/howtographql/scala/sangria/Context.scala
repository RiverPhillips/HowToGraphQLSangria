package com.howtographql.scala.sangria

import com.howtographql.scala.sangria.models.{AuthenticationException, AuthorizationException, User}

import scala.concurrent.Await
import scala.concurrent.duration.Duration

case class Context(dao: DAO, currentUser: Option[User] = None){
  def ensureAuthenticated() =
    if(currentUser.isEmpty)
      throw AuthorizationException("You do not have permission to access this resource")

  def login(email: String, password: String): User = {
    val userOpt = Await.result(dao.authenticate(email, password), Duration.Inf)
    userOpt.getOrElse(
      throw AuthenticationException("Invalid email or password")
    )
  }
}