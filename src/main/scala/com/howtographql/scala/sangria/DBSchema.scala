package com.howtographql.scala.sangria

import java.sql.Timestamp
import java.time.{LocalDateTime, Month}

import com.howtographql.scala.sangria.models.{Link, User, Vote}
import slick.jdbc.H2Profile.api._

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps


object DBSchema {
  implicit val dateTimeColumn = MappedColumnType.base[LocalDateTime, Timestamp](
    dt => Timestamp.valueOf(dt),
    ts => ts.toLocalDateTime
  )

  class UserTable(tag: Tag) extends Table[User](tag, "USERS") {
    def id = column[Int]("ID", O.PrimaryKey, O.AutoInc)
    def name = column[String]("NAME")
    def email = column[String]("EMAIL")
    def password = column[String]("PASSWORD")
    def createdAt = column[LocalDateTime]("CREATED_AT")

    def * = (id, name, email, password, createdAt).mapTo[User]
  }

  val Users = TableQuery[UserTable]

  class LinksTable(tag: Tag) extends Table[Link](tag, "LINKS"){

    def id = column[Int]("ID", O.PrimaryKey, O.AutoInc)
    def url = column[String]("URL")
    def description = column[String]("DESCRIPTION")

    def postedBy = column[Int]("USER_ID")
    def createdAt = column[LocalDateTime]("CREATED_AT")

    def postedByFk = foreignKey("postedBy_FK", postedBy, Users)(_.id)

    def * = (id, url, description, postedBy, createdAt).mapTo[Link]
  }

  val Links = TableQuery[LinksTable]

  class VoteTable(tag: Tag) extends Table[Vote](tag, "VOTES") {
    def id = column[Int]("ID", O.PrimaryKey, O.AutoInc)
    def createdAt = column[LocalDateTime]("CREATED_AT")
    def userId = column[Int]("USER_ID")
    def linkId = column[Int]("VOTE_ID")

    private def userFK = foreignKey("user_FK", userId, Users)(_.id)

    private def linkFK = foreignKey("link_FK", linkId, Links)(_.id)

    def * = (id, userId, linkId, createdAt).mapTo[Vote]
  }

  val Votes = TableQuery[VoteTable]

  val databaseSetup = DBIO.seq(
    Users.schema.create,
    Links.schema.create,
    Votes.schema.create,

    Users forceInsertAll Seq(
      User(1, "River Phillips", "riverphillips1@gmail.com", "test"),
      User(2, "John Smith", "john.smith@test.com", "p@55w0rd")
    ),

    Links forceInsertAll Seq(
      Link(1, "https://howtographql.com", "Awesome community driven GraphQL tutorial", 1, LocalDateTime.of(2019, Month.JANUARY, 20, 0, 0)),
      Link(2, "https://graphql.org", "Official GraphQL web page", 1, LocalDateTime.of(2019, Month.APRIL, 1, 0, 0)),
      Link(3, "https://facebook.github.io/graphql", "GraphQL Specification", 2, LocalDateTime.of(2018, Month.DECEMBER, 20, 0, 0))
    ),

    Votes forceInsertAll Seq (
      Vote(id = 1, userId = 1, linkId = 1),
      Vote(id = 2, userId = 1, linkId = 2),
      Vote(id = 3, userId = 1, linkId = 3),
      Vote(id = 4, userId = 2, linkId = 2)
    )
  )


  def createDatabase: DAO = {
    val db = Database.forConfig("h2mem")

    Await.result(db.run(databaseSetup), 10 seconds)

    new DAO(db)
  }


}
