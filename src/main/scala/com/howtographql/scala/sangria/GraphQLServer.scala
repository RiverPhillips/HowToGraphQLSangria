package com.howtographql.scala.sangria

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.howtographql.scala.sangria.models.{AuthenticationException, AuthorizationException}
import sangria.ast.Document
import sangria.execution._
import sangria.marshalling.sprayJson._
import sangria.parser.QueryParser
import spray.json.{JsObject, JsString, JsValue}

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

object GraphQLServer {

  private val dao = DBSchema.createDatabase

  val ErrorHandler = ExceptionHandler {
    case (_, AuthenticationException(message)) => HandledException(message)
    case (_, AuthorizationException(message)) => HandledException(message)
  }

  def endpoint(requestJson: JsValue)(implicit ec: ExecutionContext): Route = {
    val JsObject(fields) = requestJson

    val JsString(query) = fields("query")

    QueryParser.parse(query) match {
      case Success(queryAst) =>
        val operation = fields.get("operationName") collect {
          case JsString(op) => op
        }

        val variables = fields.get("variables") match {
          case Some(obj: JsObject) => obj
          case _ => JsObject.empty
        }

        complete(executeGraphQlQuery(queryAst, operation, variables))
      case Failure(error) =>
        complete(BadRequest, JsObject("error" -> JsString(error.getMessage)))
    }
  }

  def executeGraphQlQuery(query: Document, operation: Option[String], vars: JsObject)(implicit ec: ExecutionContext): ToResponseMarshallable = {
    Executor.execute(
      GraphQLSchema.SchemaDefinition,
      query,
      Context(dao),
      variables = vars,
      operationName = operation,
      deferredResolver = GraphQLSchema.Resolver,
      exceptionHandler = ErrorHandler,

      middleware = AuthMiddleware :: Nil
    ).map(OK -> _)
      .recover {
        case error: QueryAnalysisError => BadRequest -> error.resolveError
        case error: ErrorWithResolver => InternalServerError -> error.resolveError
      }
  }
}
