package com.howtographql.scala.sangria
import slick.jdbc.H2Profile.api._
import DBSchema._

class DAO(db: Database) {
  def allLinks = db.run(Links.result)
}
