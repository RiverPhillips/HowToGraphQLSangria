package com.howtographql.scala.sangria

import com.howtographql.scala.sangria.models.Authorized
import sangria.execution.{Middleware, MiddlewareBeforeField, MiddlewareQueryContext}
import sangria.schema
import sangria.schema.Action

object AuthMiddleware extends Middleware[Context] with MiddlewareBeforeField[Context]{
  override type QueryVal = Unit
  override type FieldVal = Unit


  override def beforeQuery(context: MiddlewareQueryContext[Context, _, _]): Unit = ()

  override def afterQuery(queryVal: Unit, context: MiddlewareQueryContext[Context, _, _]): Unit = ()

  override def beforeField(queryVal: Unit, mctx: MiddlewareQueryContext[Context, _, _], ctx: schema.Context[Context, _]) = {
    val requiredAuth = ctx.field.tags contains Authorized

    if(requiredAuth) ctx.ctx.ensureAuthenticated()

    continue
  }
}
