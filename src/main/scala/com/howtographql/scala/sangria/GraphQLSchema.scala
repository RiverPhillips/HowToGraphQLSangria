package com.howtographql.scala.sangria

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import com.howtographql.scala.sangria.models._
import sangria.ast.StringValue
import sangria.execution.deferred.{DeferredResolver, Fetcher, Relation, RelationIds}
import sangria.macros.derive._
import sangria.schema.{Field, IntType, ObjectType, _}
import sangria.marshalling.sprayJson._
import spray.json.DefaultJsonProtocol._

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

  implicit val authProviderEmailFormat = jsonFormat2(AuthProviderEmail)
  implicit val authProviderSignupDataFormat = jsonFormat1(AuthProviderSignupData)

  val IdentifiableType = InterfaceType(
    "Identifiable",
    fields[Unit, Identifiable](
      Field("id", IntType, resolve = _.value.id)
    )
  )

  implicit val AuthProviderEmailInputType: InputObjectType[AuthProviderEmail] = deriveInputObjectType[AuthProviderEmail](
    InputObjectTypeName("AUTH_PROVIDER_EMAIL")
  )

  lazy val AuthProviderSignupDataInputType: InputObjectType[AuthProviderSignupData] = deriveInputObjectType[AuthProviderSignupData]()

  lazy val LinkType : ObjectType[Unit, Link] = deriveObjectType[Unit, Link](
    Interfaces(IdentifiableType),
    ReplaceField("createdAt", Field("createdAt", GraphQLDateTime, resolve = _.value.createdAt)),
    ReplaceField("postedBy",
      Field("postedBy", UserType, resolve = c => usersFetcher.defer(c.value.postedBy))
    ),
    AddFields(
      Field("votes", ListType(VoteType), resolve = c => votesFetcher.deferRelSeq(voteByLinkRel, c.value.id))
    )
  )

  lazy val UserType: ObjectType[Unit, User] = deriveObjectType[Unit, User](
    Interfaces(IdentifiableType),
    ReplaceField("createdAt", Field("createdAt", GraphQLDateTime, resolve = _.value.createdAt)),
    AddFields(
      Field("links", ListType(LinkType), resolve = c => linksFetcher.deferRelSeq(linkByUserRel, c.value.id)),
      Field("votes", ListType(VoteType), resolve = c => votesFetcher.deferRelSeq(votesByUserRel, c.value.id))
    )
  )

  val VoteType: ObjectType[Unit, Vote] = deriveObjectType[Unit, Vote](
    Interfaces(IdentifiableType),
    ReplaceField("createdAt", Field("createdAt", GraphQLDateTime, resolve = _.value.createdAt)),
    ExcludeFields("userId", "linkId"),
    AddFields(
      Field("user", UserType, resolve = c => usersFetcher.defer(c.value.userId)),
      Field("link", LinkType, resolve = c => linksFetcher.defer(c.value.linkId))
    )
  )

  val linkByUserRel = Relation[Link, Int]("byUser", l => Seq(l.postedBy))
  val votesByUserRel = Relation[Vote, Int]("byUser", v => Seq(v.userId))
  val voteByLinkRel = Relation[Vote, Int]("byLink", v => Seq(v.linkId))


  val linksFetcher = Fetcher.rel(
    (ctx: Context, ids: Seq[Int]) => ctx.dao.getLinks(ids),
    (ctx: Context, ids: RelationIds[Link]) => ctx.dao.getLinksByUserIds(ids(linkByUserRel))
  )

  val usersFetcher = Fetcher(
    (ctx: Context, ids: Seq[Int]) => ctx.dao.getUsers(ids)
  )

  val votesFetcher = Fetcher.rel(
    (ctx: Context, ids: Seq[Int]) => ctx.dao.getVotes(ids),
    (ctx: Context, ids: RelationIds[Vote]) => ctx.dao.getVotesByRelationsId(ids)
  )

  val Resolver = DeferredResolver.fetchers(linksFetcher, usersFetcher, votesFetcher)


  val Id = Argument("id", IntType)
  val Ids = Argument("ids", ListInputType(IntType))
  val NameArg = Argument("name", StringType)
  val AuthProviderArg = Argument("authProvider", AuthProviderSignupDataInputType)

  val UrlArg = Argument("url", StringType)
  val DescriptionArg = Argument("description", StringType)
  val PostedByIdArg = Argument("postedById", IntType)

  val UserId = Argument("userId", IntType)
  val LinkId = Argument("linkId", IntType)

  val Mutation = ObjectType(
    "Mutation",
    fields[Context, Unit](
      Field("createUser",
        UserType,
        arguments = NameArg :: AuthProviderArg :: Nil,
        resolve = c => c.ctx.dao.createUser(c.arg(NameArg), c.arg(AuthProviderArg))
      ),
      Field("createLink",
        LinkType,
        arguments = UrlArg :: DescriptionArg :: PostedByIdArg :: Nil,
        resolve = c => c.ctx.dao.createLink(c.arg(UrlArg), c.arg(DescriptionArg), c.arg(PostedByIdArg))
      ),
      Field("createVote",
        VoteType,
        arguments = UserId :: LinkId  :: Nil,
        resolve = c => c.ctx.dao.createVote(c.arg(UserId), c.arg(LinkId))
      )
    )
  )

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

  val SchemaDefinition = Schema(QueryType, Some(Mutation))
}
