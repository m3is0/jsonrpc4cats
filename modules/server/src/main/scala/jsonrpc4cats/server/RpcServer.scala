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

/*
 * The implementation of routing is based on https://github.com/milessabin/shapeless/blob/cb23c76516f3792659bf88809314e972fd6fb5dc/examples/src/main/scala/shapeless/examples/router.scala
 * Copyright (c) 2011 Miles Sabin
 */

package jsonrpc4cats.server

import scala.compiletime.*
import scala.compiletime.ops.any.*
import scala.compiletime.ops.boolean.*

import cats.MonadError
import cats.data.OptionT

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

  extension [F[_], A <: Coproduct](sa: RpcServer[F, A]) {
    inline def extend[B <: Coproduct](sb: RpcServer[F, B])(using
      ex: ExtendBy[A, B],
      ev: DistinctMethods[F, Extend[A, B]] =:= true
    ): RpcServer[F, Extend[A, B]] =
      new RpcServer[F, Extend[A, B]] {
        def apply(method: String) =
          sb(method) match {
            case Some(vb) =>
              Some(ex.left(vb))
            case _ =>
              sa(method).map(ex.right)
          }
      }

    inline def :+:[B <: Coproduct](sb: RpcServer[F, B])(using
      ex: ExtendBy[A, B],
      ev: DistinctMethods[F, Extend[A, B]] =:= true
    ): RpcServer[F, Extend[A, B]] =
      sa.extend(sb)

    inline def add[K <: String & Singleton, P <: Product, E, B](m: RpcMethod[F, K, P, E, B])(using
      ex: ExtendBy[A, RpcMethod[F, K, P, E, B] :+: CNil],
      ev: DistinctMethods[F, Extend[A, RpcMethod[F, K, P, E, B] :+: CNil]] =:= true
    ): RpcServer[F, Extend[A, RpcMethod[F, K, P, E, B] :+: CNil]] =
      sa.extend(RpcServer.add[F, K, P, E, B](m))
  }

  extension [F[_], A <: Coproduct](sa: RpcServer[F, A])(using F: MonadError[F, Throwable]) {
    def handle[J](req: String)(using
      handleRequest: RequestHandler[F, A, J]
    ): OptionT[F, J] =
      OptionT(handleRequest(req, sa, _ => F.pure(())))

    def handle[J](req: String, onError: RequestHandler.OnError[F])(using
      handleRequest: RequestHandler[F, A, J]
    ): OptionT[F, J] =
      OptionT(handleRequest(req, sa, onError))
  }

}
