package com.howtographql.scala.sangria

import java.sql.Timestamp
import java.time.{LocalDate, LocalDateTime, Month}

import com.howtographql.scala.sangria.models.Link
import slick.jdbc.H2Profile.api._

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps


object DBSchema {
  implicit val dateTimeColumn = MappedColumnType.base[LocalDateTime, Timestamp](
    dt => Timestamp.valueOf(dt),
    ts => ts.toLocalDateTime
  )

  class LinksTable(tag: Tag) extends Table[Link](tag, "LINKS"){

    def id = column[Int]("ID", O.PrimaryKey, O.AutoInc)
    def url = column[String]("URL")
    def description = column[String]("DESCRIPTION")
    def createdAt = column[LocalDateTime]("CREATED_AT")

    def * = (id, url, description, createdAt).mapTo[Link]
  }

  val Links = TableQuery[LinksTable]

  val databaseSetup = DBIO.seq(
    Links.schema.create,

    Links forceInsertAll Seq(
      Link(1, "https://howtographql.com", "Awesome community driven GraphQL tutorial", LocalDateTime.of(2019, Month.JANUARY, 20, 0, 0)),
      Link(2, "https://graphql.org", "Official GraphQL web page", LocalDateTime.of(2019, Month.APRIL, 1, 0, 0)),
      Link(3, "https://facebook.github.io/graphql", "GraphQL Specification", LocalDateTime.of(2018, Month.DECEMBER, 20, 0, 0))
    )
  )


  def createDatabase: DAO = {
    val db = Database.forConfig("h2mem")

    Await.result(db.run(databaseSetup), 10 seconds)

    new DAO(db)

  }


}
