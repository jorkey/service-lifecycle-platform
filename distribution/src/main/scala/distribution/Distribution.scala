package distribution

import akka.http.scaladsl.model.{HttpRequest, StatusCodes}
import akka.http.scaladsl.server.Directives.complete
import akka.http.scaladsl.server.{ExceptionHandler, RouteResult}
import akka.http.scaladsl.server.directives.Credentials
import com.vyulabs.update.users.{PasswordHash, UserInfo, UsersCredentials}
import distribution.graphql.{Graphql, GraphqlContext}
import org.slf4j.LoggerFactory

class Distribution[Context <: GraphqlContext](usersCredentials: UsersCredentials, protected val graphQL: Graphql[Context]) {
  protected implicit val log = LoggerFactory.getLogger(this.getClass)

  protected val exceptionHandler = ExceptionHandler {
    case ex =>
      log.error("Exception", ex)
      complete(StatusCodes.InternalServerError, s"Server error: ${ex.getMessage}")
  }

  def authenticate(credentials: Credentials): Option[UserInfo] = {
    credentials match {
      case p@Credentials.Provided(userName) =>
        usersCredentials.getCredentials(userName) match {
          case Some(userCredentials) if p.verify(userCredentials.password.hash,
            PasswordHash.generatePasswordHash(_, userCredentials.password.salt)) =>
            Some(UserInfo(userName, userCredentials.role))
          case _ =>
            None
        }
      case _ => None
    }
  }

  def requestLogger(req: HttpRequest): String = {
    "Request: " + req.toString()
  }

  def resultLogger(res: RouteResult): String = {
    "Result: " + res.toString()
  }
}
