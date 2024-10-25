/*
 * Copyright 2024 m3is0
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

  // The server will respond with '200 OK' for any valid request
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

  // The server will respond with '200 OK' for any User
  def sum[F[_]](using F: Applicative[F]) =
    RpcMethod.withAuth[F, User, "sec.sum", (Int, Int), RpcErr, String] {
      case (user, (a, b)) =>
        F.pure(Right(s"${user.name}: ${a.toLong + b.toLong}"))
    }

  // The server will respond with '401 Unauthorized' if User.isAdmin == false
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

  // A dummy AuthMiddleware
  val authMiddleware: AuthMiddleware[IO, User] =
    AuthMiddleware[IO, User](
      Kleisli(_ => OptionT.pure[IO](User("User123", false)))
    )

  // HttpApp that can be run with Http4s
  val httpApp: HttpApp[IO] =
    Router(
      "/rpc" -> RpcService.httpRoutes[Json](PublicRpc.api[IO]),
      "/secured/rpc" -> authMiddleware(RpcService.authedRoutes[Json](SecuredRpc.api[IO]))
    ).orNotFound
}
