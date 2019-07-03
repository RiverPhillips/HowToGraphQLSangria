package com.howtographql.scala.sangria

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import com.howtographql.scala.sangria.models.{Identifiable, Link, LocalDateTimeCoerceViolation, User, Vote}
import sangria.ast.StringValue
import sangria.execution.deferred.{DeferredResolver, Fetcher, Relation, RelationIds}
import sangria.macros.derive._
import sangria.schema.{Field, IntType, ObjectType, _}

import scala.util.{Success, Try}

object GraphQLSchema {
  private def parseLocalDateTime(s: String) = Try(LocalDateTime.parse(s)) match {
    case Success(date) => Right(date)
    case _ => Left(LocalDateTimeCoerceViolation)
  }

  implicit val GraphQLDateTime = ScalarType[LocalDateTime](
    "DateTime",
    coerceOutput = (dt, _) => dt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
    coerceUserInput = {
      case s: String => parseLocalDateTime(s)
      case _ => Left(LocalDateTimeCoerceViolation)
    },
    coerceInput = {
      case stringValue: StringValue => parseLocalDateTime(stringValue.value)
      case _ => Left(LocalDateTimeCoerceViolation)
    }
  )

  val IdentifiableType = InterfaceType(
    "Identifiable",
    fields[Unit, Identifiable](
      Field("id", IntType, resolve = _.value.id)
    )
  )

  lazy val LinkType : ObjectType[Unit, Link] = deriveObjectType[Unit, Link](
    Interfaces(IdentifiableType),
    ReplaceField("createdAt", Field("createdAt", GraphQLDateTime, resolve = _.value.createdAt)),
    ReplaceField("postedBy",
      Field("postedBy", UserType, resolve = c => usersFetcher.defer(c.value.postedBy))
    )
  )

  lazy val UserType: ObjectType[Unit, User] = deriveObjectType[Unit, User](
    Interfaces(IdentifiableType),
    ReplaceField("createdAt", Field("createdAt", GraphQLDateTime, resolve = _.value.createdAt)),
    AddFields(
      Field("links", ListType(LinkType), resolve = c => linksFetcher.deferRelSeq(linkByUserRel, c.value.id))
    )
  )

  val VoteType: ObjectType[Unit, Vote] = deriveObjectType[Unit, Vote](
    Interfaces(IdentifiableType),
    ReplaceField("createdAt", Field("createdAt", GraphQLDateTime, resolve = _.value.createdAt))
  )

  val linkByUserRel = Relation[Link, Int]("byUser", l => Seq(l.postedBy))

  val linksFetcher = Fetcher.rel(
    (ctx: Context, ids: Seq[Int]) => ctx.dao.getLinks(ids),
    (ctx: Context, ids: RelationIds[Link]) => ctx.dao.getLinksByUserIds(ids(linkByUserRel))
  )

  val usersFetcher = Fetcher(
    (ctx: Context, ids: Seq[Int]) => ctx.dao.getUsers(ids)
  )

  val votesFetcher = Fetcher(
    (ctx: Context, ids: Seq[Int]) => ctx.dao.getVotes(ids)
  )

  val Resolver = DeferredResolver.fetchers(linksFetcher, usersFetcher, votesFetcher)


  val Id = Argument("id", IntType)
  val Ids = Argument("ids", ListInputType(IntType))

  val QueryType = ObjectType(
    "Query",
    fields[Context, Unit](
      Field("allLinks", ListType(LinkType), resolve = c => c.ctx.dao.allLinks),
      Field(
        "link",
        OptionType(LinkType),
        arguments = Id :: Nil,
        resolve = c => linksFetcher.deferOpt(c.arg(Id))
      ),
      Field(
        "links",
        ListType(LinkType),
        arguments = Ids :: Nil,
        resolve = c => linksFetcher.deferSeq(c.arg(Ids))
      ),
      Field("allUsers", ListType(UserType), resolve = c => c.ctx.dao.allUsers),
      Field(
        "user",
        OptionType(UserType),
        arguments = Id :: Nil,
        resolve = c => usersFetcher.deferOpt(c.arg(Id))
      ),
      Field(
        "users",
        ListType(UserType),
        arguments = Ids :: Nil,
        resolve = c => usersFetcher.deferSeq(c.arg(Ids))
      ),
      Field("allVotes", ListType(VoteType), resolve = c => c.ctx.dao.allVotes),
      Field(
        "vote",
        OptionType(VoteType),
        arguments = Id :: Nil,
        resolve = c => votesFetcher.deferOpt(c.arg(Id))
      ),
      Field(
        "votes",
        ListType(VoteType),
        arguments = Ids :: Nil,
        resolve = c => votesFetcher.deferSeq(c.arg(Ids))
      )
    )
  )

  val SchemaDefinition = Schema(QueryType)
}
