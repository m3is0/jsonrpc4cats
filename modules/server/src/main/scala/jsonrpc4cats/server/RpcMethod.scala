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

trait RpcMethod[F[_], K <: String & Singleton, P <: Product, E, R] {
  def apply(p: P): F[Either[E, R]]
}

object RpcMethod {

  def instance[F[_], K <: String & Singleton, P <: Product, E, R](
    f: P => F[Either[E, R]]
  ): RpcMethod[F, K, P, E, R] =
    new RpcMethod[F, K, P, E, R] {
      def apply(p: P): F[Either[E, R]] =
        f(p)
    }

  def withAuth[F[_]: Applicative, U, K <: String & Singleton, P <: Product, E, R](
    f: (U, P) => F[Either[E, R]]
  ): RpcMethod[[x] =>> Auth[F, U, x], K, P, E, R] =
    instance[[x] =>> Auth[F, U, x], K, P, E, R](p => Auth.allowAll[U](u => f(u, p)))

  def withAuthIf[F[_]: Applicative, U, K <: String & Singleton, P <: Product, E, R](cond: U => Boolean)(
    f: (U, P) => F[Either[E, R]]
  ): RpcMethod[[x] =>> Auth[F, U, x], K, P, E, R] =
    instance[[x] =>> Auth[F, U, x], K, P, E, R](p => Auth.allowIf[U](cond)(u => f(u, p)))

}
