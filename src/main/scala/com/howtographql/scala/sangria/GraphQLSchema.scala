package com.howtographql.scala.sangria

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import com.howtographql.scala.sangria.models.{Link, LocalDateTimeCoerceViolation}
import sangria.ast.StringValue
import sangria.execution.deferred.{DeferredResolver, Fetcher, HasId}
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

  implicit val LinkType = deriveObjectType[Unit, Link](
    ReplaceField("createdAt", Field("createdAt", GraphQLDateTime, resolve = _.value.createdAt))
  )
  implicit val linkHasId = HasId[Link, Int](_.id)



  val linksFetcher = Fetcher(
    (ctx: Context, ids: Seq[Int]) => ctx.dao.getLinks(ids)
  )

  val Resolver = DeferredResolver.fetchers(linksFetcher)


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
      )
    )
  )

  val SchemaDefinition = Schema(QueryType)
}
