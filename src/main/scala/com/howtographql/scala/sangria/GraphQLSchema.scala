package com.howtographql.scala.sangria

import com.howtographql.scala.sangria.models.Link
import sangria.macros.derive._
import sangria.schema.{Field, IntType, ObjectType, _}

object GraphQLSchema {
  implicit val LinkType = deriveObjectType[Unit, Link]()

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
        resolve = c => c.ctx.dao.getLink(c.arg(Id))
      ),
      Field(
        "links",
        ListType(LinkType),
        arguments = Ids :: Nil,
        resolve = c => c.ctx.dao.getLinks(c.arg(Ids))
      )
    )
  )

  val SchemaDefinition = Schema(QueryType)
}
