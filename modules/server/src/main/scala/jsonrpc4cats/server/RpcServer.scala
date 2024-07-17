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

package jsonrpc4cats.server

import scala.compiletime.*
import scala.compiletime.ops.any.*
import scala.compiletime.ops.boolean.*

import cats.Applicative

import jsonrpc4cats.server.internal.*

trait RpcServer[F[_], A <: Coproduct] {
  def apply(method: String): Option[A]
}

object RpcServer {
  import Coproduct.*

  type NoMethod[F[_], A <: Coproduct, K] <: Boolean =
    A match {
      case CNil => true
      case RpcMethod[F, k, ?, ?, ?] :+: t => (k != K) && NoMethod[F, t, K]
    }

  type DistinctMethods[F[_], A <: Coproduct] <: Boolean =
    A match {
      case CNil => true
      case RpcMethod[F, k, ?, ?, ?] :+: t => NoMethod[F, t, k] && DistinctMethods[F, t]
    }

  inline def add[F[_], K <: String & Singleton, P <: Product, E, R](
      m: RpcMethod[F, K, P, E, R]
  ): RpcServer[F, RpcMethod[F, K, P, E, R] :+: CNil] =
    new RpcServer[F, RpcMethod[F, K, P, E, R] :+: CNil] {
      def apply(method: String) =
        if constValue[K] == method
        then Some(Inl(m))
        else None
    }

  extension [F[_]: Applicative, L <: Coproduct](srvL: RpcServer[F, L]) {
    inline def extend[R <: Coproduct](srvR: RpcServer[F, R])(using
        ex: ExtendBy[L, R],
        ev: DistinctMethods[F, Extend[L, R]] =:= true
    ): RpcServer[F, Extend[L, R]] =
      new RpcServer[F, Extend[L, R]] {
        def apply(method: String) =
          srvR(method) match {
            case Some(vb) =>
              Some(ex.left(vb))
            case _ =>
              srvL(method).map(ex.right)
          }
      }

    inline def add[K <: String & Singleton, P <: Product, E, R](m: RpcMethod[F, K, P, E, R])(using
        ex: ExtendBy[L, RpcMethod[F, K, P, E, R] :+: CNil],
        ev: DistinctMethods[F, Extend[L, RpcMethod[F, K, P, E, R] :+: CNil]] =:= true
    ): RpcServer[F, Extend[L, RpcMethod[F, K, P, E, R] :+: CNil]] =
      srvL.extend(RpcServer.add[F, K, P, E, R](m))

    def handle[J](req: String)(using
        handleRequest: RequestHandler[F, L, J]
    ): F[Option[J]] =
      handleRequest(req, srvL, _ => Applicative[F].pure(()))

    def handle[J](req: String, onError: RequestHandler.OnError[F, J])(using
        handleRequest: RequestHandler[F, L, J]
    ): F[Option[J]] =
      handleRequest(req, srvL, onError)
  }
}
