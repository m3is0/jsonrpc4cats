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

package jsonrpc4cats.http4s

import cats.effect.Concurrent
import cats.syntax.all.*
import org.http4s.AuthedRoutes
import org.http4s.HttpRoutes
import org.http4s.MediaType
import org.http4s.Response
import org.http4s.Status
import org.http4s.dsl.io.*
import org.http4s.headers.`Content-Type`

import jsonrpc4cats.json.JsonPrinter
import jsonrpc4cats.server.RpcServer
import jsonrpc4cats.server.internal.Auth
import jsonrpc4cats.server.internal.Coproduct
import jsonrpc4cats.server.internal.RequestHandler

object RpcService {

  /**
   * Creates HttpRoutes[F] for RpcServer[F, A]
   */
  def httpRoutes[J]: HttpRoutesPartiallyApplied[J] = new HttpRoutesPartiallyApplied[J]()

  private[http4s] final class HttpRoutesPartiallyApplied[J](val dummie: Boolean = true) {
    def apply[F[_], A <: Coproduct](srv: RpcServer[F, A])(using
      JsonPrinter[J],
      RequestHandler[F, A, J]
    )(using F: Concurrent[F]): HttpRoutes[F] =
      apply[F, A](srv, _ => F.pure(()))

    def apply[F[_], A <: Coproduct](
      srv: RpcServer[F, A],
      onError: RequestHandler.OnError[F]
    )(using RequestHandler[F, A, J])(using
      F: Concurrent[F],
      printJson: JsonPrinter[J]
    ): HttpRoutes[F] =
      HttpRoutes.of[F] {
        case req @ POST -> Root =>
          req.headers.get[`Content-Type`].map(_.mediaType) match {
            case Some(MediaType.application.json) =>
              req.decode[String] { str =>
                srv.handle[J](str, onError).value.map {
                  case Some(json) =>
                    Response[F](Status.Ok)
                      .withEntity(printJson(json))
                      .withContentType(`Content-Type`(MediaType.application.json))
                  case _ =>
                    Response[F](Status.Accepted)
                }
              }
            case _ =>
              F.pure(Response[F](Status.UnsupportedMediaType))
          }
        case _ -> Root =>
          F.pure(Response[F](Status.MethodNotAllowed))
      }
  }

  /**
   * Creates AuthedRoutes[U, F] for RpcServer[[x] =>> Auth[F, U, x], A]
   */
  def authedRoutes[J]: AuthedRoutesPartiallyApplied[J] = new AuthedRoutesPartiallyApplied[J]()

  private[http4s] final class AuthedRoutesPartiallyApplied[J](val dummie: Boolean = true) {
    def apply[F[_], A <: Coproduct, U](srv: RpcServer[[x] =>> Auth[F, U, x], A])(using
      JsonPrinter[J],
      RequestHandler[[x] =>> Auth[F, U, x], A, J]
    )(using F: Concurrent[F]): AuthedRoutes[U, F] =
      apply[F, A, U](srv, _ => F.pure(()))

    def apply[F[_], A <: Coproduct, U](
      srv: RpcServer[[x] =>> Auth[F, U, x], A],
      onError: RequestHandler.OnError[F]
    )(using RequestHandler[[x] =>> Auth[F, U, x], A, J])(using
      F: Concurrent[F],
      printJson: JsonPrinter[J]
    ): AuthedRoutes[U, F] =
      AuthedRoutes.of[U, F] {
        case req @ POST -> Root as user =>
          req.req.headers.get[`Content-Type`].map(_.mediaType) match {
            case Some(MediaType.application.json) =>
              req.req.decode[String] { str =>
                srv.handle[J](str, e => Auth.liftF[U](onError(e))).value.run(user).value.map {
                  case Some(Some(json)) =>
                    Response[F](Status.Ok)
                      .withEntity(printJson(json))
                      .withContentType(`Content-Type`(MediaType.application.json))
                  case Some(_) =>
                    Response[F](Status.Accepted)
                  case _ =>
                    Response[F](Status.Unauthorized)
                }
              }
            case _ =>
              F.pure(Response[F](Status.UnsupportedMediaType))
          }
        case _ -> Root as _ =>
          F.pure(Response[F](Status.MethodNotAllowed))
      }
  }

}
