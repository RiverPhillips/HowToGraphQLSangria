package com.howtographql.scala.sangria

import java.sql.Timestamp
import java.time.{LocalDate, LocalDateTime, Month}

import com.howtographql.scala.sangria.models.{Link, User, Vote}
import slick.jdbc.H2Profile.api._
import slick.lifted.ForeignKeyQuery

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

  class UserTable(tag: Tag) extends Table[User](tag, "USERS") {
    def id = column[Int]("ID", O.PrimaryKey, O.AutoInc)
    def name = column[String]("NAME")
    def email = column[String]("EMAIL")
    def password = column[String]("PASSWORD")
    def createdAt = column[LocalDateTime]("CREATED_AT")

    def * = (id, name, email, password, createdAt).mapTo[User]
  }

  val Users = TableQuery[UserTable]

  class VoteTable(tag: Tag) extends Table[Vote](tag, "VOTES") {
    def id = column[Int]("ID", O.PrimaryKey, O.AutoInc)
    def createdAt = column[LocalDateTime]("CREATED_AT")
    def userId = column[Int]("USER_ID")
    def linkId = column[Int]("VOTE_ID")

    private def User = foreignKey("USER_FK", userId, Users)(_.id)
    private def Link = foreignKey("LINK_FK", linkId, Links)(_.id)

    def * = (id, createdAt, userId, linkId).mapTo[Vote]
  }

  val Votes = TableQuery[VoteTable]

  val databaseSetup = DBIO.seq(
    Links.schema.create,
    Users.schema.create,
    Votes.schema.create,

    Links forceInsertAll Seq(
      Link(1, "https://howtographql.com", "Awesome community driven GraphQL tutorial", LocalDateTime.of(2019, Month.JANUARY, 20, 0, 0)),
      Link(2, "https://graphql.org", "Official GraphQL web page", LocalDateTime.of(2019, Month.APRIL, 1, 0, 0)),
      Link(3, "https://facebook.github.io/graphql", "GraphQL Specification", LocalDateTime.of(2018, Month.DECEMBER, 20, 0, 0))
    ),

    Users forceInsertAll Seq(
      User(1, "River", "Phillips", "test")
    ),

    Votes forceInsertAll Seq (
      Vote(id = 1, userId = 1, linkId = 3)
    )
  )


  def createDatabase: DAO = {
    val db = Database.forConfig("h2mem")

    Await.result(db.run(databaseSetup), 10 seconds)

    new DAO(db)
  }


}
