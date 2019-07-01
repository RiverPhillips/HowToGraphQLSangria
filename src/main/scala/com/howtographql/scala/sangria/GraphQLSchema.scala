package com.howtographql.scala.sangria

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import com.howtographql.scala.sangria.models.{Link, LocalDateTimeCoerceViolation, User, Vote, Identifiable}
import sangria.ast.StringValue
import sangria.execution.deferred.{DeferredResolver, Fetcher}
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

  implicit val LinkType = deriveObjectType[Unit, Link](
    Interfaces(IdentifiableType),
    ReplaceField("createdAt", Field("createdAt", GraphQLDateTime, resolve = _.value.createdAt))
  )

  implicit val UserType = deriveObjectType[Unit, User](
    Interfaces(IdentifiableType),
    ReplaceField("createdAt", Field("createdAt", GraphQLDateTime, resolve = _.value.createdAt))
  )

  implicit val VoteType = deriveObjectType[Unit, Vote](
    Interfaces(IdentifiableType),
    ReplaceField("createdAt", Field("createdAt", GraphQLDateTime, resolve = _.value.createdAt))
  )

  val linksFetcher = Fetcher(
    (ctx: Context, ids: Seq[Int]) => ctx.dao.getLinks(ids)
  )

  val userFetcher = Fetcher(
    (ctx: Context, ids: Seq[Int]) => ctx.dao.getUsers(ids)
  )

  val voteFetcher = Fetcher(
    (ctx: Context, ids: Seq[Int]) => ctx.dao.getVotes(ids)
  )

  val Resolver = DeferredResolver.fetchers(linksFetcher, userFetcher, voteFetcher)


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
        resolve = c => userFetcher.deferOpt(c.arg(Id))
      ),
      Field(
        "users",
        ListType(UserType),
        arguments = Ids :: Nil,
        resolve = c => userFetcher.deferSeq(c.arg(Ids))
      ),
      Field("allVotes", ListType(VoteType), resolve = c => c.ctx.dao.allVotes),
      Field(
        "vote",
        OptionType(VoteType),
        arguments = Id :: Nil,
        resolve = c => voteFetcher.deferOpt(c.arg(Id))
      ),
      Field(
        "votes",
        ListType(VoteType),
        arguments = Ids :: Nil,
        resolve = c => voteFetcher.deferSeq(c.arg(Ids))
      )
    )
  )

  val SchemaDefinition = Schema(QueryType)
}
