package com.howtographql.scala.sangria

import java.time.LocalDateTime

import sangria.validation.Violation

package object models {

  case class Link(id: Int, url: String, description: String, createdAt: LocalDateTime)

  case object LocalDateTimeCoerceViolation extends Violation {
    def errorMessage: String = "Error parsing DateTime"
  }
}
