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
import cats.data.Kleisli
import cats.data.OptionT

type Auth[F[_], U, A] = Kleisli[[x] =>> OptionT[F, x], U, A]

object Auth {

  def liftF[U]: LiftFPartiallyApplied[U] = new LiftFPartiallyApplied[U]

  private[server] final class LiftFPartiallyApplied[U](val dummie: Boolean = true) {
    def apply[F[_]: Applicative, A](fa: F[A]): Auth[F, U, A] =
      Kleisli(_ => OptionT.liftF(fa))
  }

  def allowIf[U]: AllowIfPartiallyApplied[U] = new AllowIfPartiallyApplied[U]

  private[server] final class AllowIfPartiallyApplied[U](val dummie: Boolean = true) {
    def apply[F[_]: Applicative, A](cond: U => Boolean)(f: U => F[A]): Auth[F, U, A] =
      Kleisli { u =>
        cond(u) match {
          case true => OptionT.liftF(f(u))
          case _ => OptionT.fromOption[F](None)
        }
      }
  }

  def allowAll[U]: AllowAllPartiallyApplied[U] = new AllowAllPartiallyApplied[U]

  private[server] final class AllowAllPartiallyApplied[U](val dummie: Boolean = true) {
    def apply[F[_]: Applicative, A](f: U => F[A]): Auth[F, U, A] =
      Kleisli(u => OptionT.liftF(f(u)))
  }

}
