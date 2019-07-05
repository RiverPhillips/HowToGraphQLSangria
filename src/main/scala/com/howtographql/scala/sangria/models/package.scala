package com.howtographql.scala.sangria

import java.time.LocalDateTime

import sangria.execution.FieldTag
import sangria.execution.deferred.HasId
import sangria.validation.Violation

package object models {

  trait Identifiable {
    val id: Int
  }

  object Identifiable {
    implicit def hasId[T <: Identifiable]: HasId[T, Int] = HasId(_.id)
  }

  case class Link(id: Int, url: String, description: String, postedBy: Int, createdAt: LocalDateTime = LocalDateTime.now) extends Identifiable

  case class User(id: Int, name: String, email: String, password: String, createdAt: LocalDateTime = LocalDateTime.now) extends Identifiable

  case class Vote(id: Int, userId: Int, linkId: Int, createdAt: LocalDateTime = LocalDateTime.now) extends Identifiable

  case class AuthProviderEmail(email: String, password: String)

  case class AuthProviderSignupData(email: AuthProviderEmail)

  case object LocalDateTimeCoerceViolation extends Violation {
    def errorMessage: String = "Error parsing DateTime"
  }

  case object Authorized extends FieldTag

  case class AuthenticationException(message: String) extends Exception(message)

  case class AuthorizationException(message: String) extends Exception(message)
}
