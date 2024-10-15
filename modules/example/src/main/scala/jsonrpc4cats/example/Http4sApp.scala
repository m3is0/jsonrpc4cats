package jsonrpc4cats.example

import cats.Applicative
import cats.MonadError
import cats.data.Kleisli
import cats.data.OptionT
import cats.effect.IO
import io.circe.Json
import org.http4s.implicits.*
import org.http4s.server.Router
import org.http4s.HttpApp
import org.http4s.server.AuthMiddleware

import jsonrpc4cats.RpcErr
import jsonrpc4cats.server.RpcMethod
import jsonrpc4cats.server.RpcServer
import jsonrpc4cats.circe.given
import jsonrpc4cats.http4s.RpcService

object PublicRpc {

  // The server will respond with HTTP 200 OK for any valid request
  def sum[F[_]](using F: Applicative[F]) =
    RpcMethod.instance[F, "pub.sum", (Int, Int), RpcErr, Long] { (a, b) =>
      F.pure(Right(a.toLong + b.toLong))
    }

  def api[F[_]: Applicative] =
    RpcServer
      .add(sum[F])

}

object SecuredRpc {

  final case class User(name: String, isAdmin: Boolean)

  // The server will respond with HTTP 200 OK for any User
  def sum[F[_]](using F: Applicative[F]) =
    RpcMethod.withAuth[F, User, "sec.sum", (Int, Int), RpcErr, String] {
      case (user, (a, b)) =>
        F.pure(Right(s"${user.name}: ${a.toLong + b.toLong}"))
    }

  // The server will respond with HTTP 401 Unauthorized if User.isAdmin == false
  def multiply[F[_]](using F: Applicative[F]) =
    RpcMethod.withAuthIf[F, User, "sec.mul", (Int, Int), RpcErr, String](_.isAdmin) {
      case (user, (a, b)) =>
        F.pure(Right(s"${user.name}: ${a.toLong * b.toLong}"))
    }

  def api[F[_]: Applicative] =
    RpcServer
      .add(sum[F])
      .add(multiply[F])
      
}

object Http4sApp {

  import SecuredRpc.User
  
  // A dummie AuthMiddleware  
  val authMiddleware: AuthMiddleware[IO, User] =
    AuthMiddleware[IO, User](
      Kleisli(_ => OptionT.fromOption[IO](Some(User("User123", false))))
    )
  
  // HttpApp that can be run with Http4s
  val httpApp: HttpApp[IO] =
    Router(
      "/rpc"         -> RpcService.httpRoutes[Json](PublicRpc.api[IO]),
      "/secured/rpc" -> authMiddleware(RpcService.authedRoutes[Json](SecuredRpc.api[IO]))
    ).orNotFound

}
