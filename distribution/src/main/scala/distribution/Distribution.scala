package distribution

import akka.http.scaladsl.model.{HttpRequest, StatusCodes}
import akka.http.scaladsl.server.Directives.complete
import akka.http.scaladsl.server.{ExceptionHandler, RouteResult}
import akka.http.scaladsl.server.directives.Credentials
import com.vyulabs.update.common.Common.UserName
import com.vyulabs.update.users.{PasswordHash, UserCredentials, UsersCredentials}
import distribution.graphql.GraphQL
import org.slf4j.LoggerFactory

class Distribution(usersCredentials: UsersCredentials, protected val graphQL: GraphQL) {
  protected implicit val log = LoggerFactory.getLogger(this.getClass)

  protected val exceptionHandler = ExceptionHandler {
    case ex =>
      log.error("Exception", ex)
      complete(StatusCodes.InternalServerError, s"Server error: ${ex.getMessage}")
  }

  def authenticate(credentials: Credentials): Option[(UserName, UserCredentials)] = {
    credentials match {
      case p@Credentials.Provided(userName) =>
        usersCredentials.getCredentials(userName) match {
          case Some(userCredentials) if p.verify(userCredentials.password.hash,
            PasswordHash.generatePasswordHash(_, userCredentials.password.salt)) =>
            Some(userName, userCredentials)
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
