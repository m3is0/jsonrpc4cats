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

import cats.Applicative

type RpcMethod1[F[_], K <: String & Singleton, P, E, R] =
  RpcMethod[F, K, Tuple1[P], E, R]

object RpcMethod1 {

  def instance[F[_], K <: String & Singleton, P, E, R](
    f: P => F[Either[E, R]]
  ): RpcMethod1[F, K, P, E, R] =
    RpcMethod.instance(p => f(p.head))

  def withAuth[F[_]: Applicative, U, K <: String & Singleton, P, E, R](
    f: (U, P) => F[Either[E, R]]
  ): RpcMethod1[[x] =>> Auth[F, U, x], K, P, E, R] =
    RpcMethod.withAuth((u, p) => f(u, p.head))

  def withAuthIf[F[_]: Applicative, U, K <: String & Singleton, P, E, R](cond: U => Boolean)(
    f: (U, P) => F[Either[E, R]]
  ): RpcMethod1[[x] =>> Auth[F, U, x], K, P, E, R] =
    RpcMethod.withAuthIf(cond)((u, p) => f(u, p.head))

}
