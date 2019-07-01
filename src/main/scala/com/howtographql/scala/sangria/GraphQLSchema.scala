package com.howtographql.scala.sangria

import com.howtographql.scala.sangria.models.Link
import sangria.execution.deferred.{DeferredResolver, Fetcher, HasId}
import sangria.macros.derive._
import sangria.schema.{Field, IntType, ObjectType, _}

object GraphQLSchema {
  implicit val LinkType = deriveObjectType[Unit, Link]()
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
