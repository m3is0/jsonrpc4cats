# Http4s Integration

## Dependencies

Add the following dependencies to your `build.sbt`:
```scala
libraryDependencies ++= Seq(
  "io.github.m3is0" %% "jsonrpc4cats-circe" % "@VERSION@",
  "io.github.m3is0" %% "jsonrpc4cats-http4s" % "@VERSION@"
)
```

## A Basic Example

An example showing how to integrate `jsonrpc4cats` with `http4s` and its built-in authentication:
```scala
package jsonrpc4cats.example

import cats.Applicative
import cats.data.Kleisli
import cats.data.OptionT
import cats.effect.IO
import io.circe.Json
import org.http4s.HttpApp
import org.http4s.server.AuthMiddleware
import org.http4s.server.Router

import jsonrpc4cats.RpcErr
import jsonrpc4cats.circe.given
import jsonrpc4cats.http4s.RpcService
import jsonrpc4cats.server.RpcMethod
import jsonrpc4cats.server.RpcServer

object PublicRpc {

  // the HTTP response will be '200 OK' for any valid request
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

  // the HTTP response will be '200 OK' for any User
  def sum[F[_]](using F: Applicative[F]) =
    RpcMethod.withAuth[F, User, "sec.sum", (Int, Int), RpcErr, String] {
      case (user, (a, b)) =>
        F.pure(Right(s"${user.name}: ${a.toLong + b.toLong}"))
    }

  // the HTTP response will be '401 Unauthorized' if User.isAdmin == false
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

  // a dummy AuthMiddleware
  val authMiddleware: AuthMiddleware[IO, User] =
    AuthMiddleware[IO, User](
      Kleisli(_ => OptionT.pure[IO](User("User123", false)))
    )

  // HttpApp, which can be run with Http4s
  val httpApp: HttpApp[IO] =
    Router(
      "/rpc" -> RpcService.httpRoutes[Json](PublicRpc.api[IO]),
      "/secured/rpc" -> authMiddleware(RpcService.authedRoutes[Json](SecuredRpc.api[IO]))
    ).orNotFound
}

```


## Request Requirements

- The request method must be 'POST'
- The 'Content-Type' header must be 'application/json' 


## Response Codes

- '405 Method Not Allowed' if the request method is not 'POST'
- '415 Unsupported Media Type' if the 'Content-Type' is not 'application/json'
- '401 Unauthorized' if the action is not authorized (controlled by the application)
- '202 Accepted' for empty responses, when all request objects are JSON-RPC notifications
- '200 OK' for all JSON-RPC responses, including errors

