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

// scalafmt: { newlines.source = keep }

package jsonrpc4cats.example

import cats.Applicative
import cats.MonadError
import cats.data.EitherT
import cats.syntax.all.*
import io.circe.Json

import jsonrpc4cats.*
import jsonrpc4cats.circe.given
import jsonrpc4cats.client.*

object CalcClient {

  import RpcCall.*

  // 1. Define errors (it's also possible to define errors in a shared module)

  sealed trait Err
  case object DivByZero extends Err

  given errFromRpcError[J]: FromRpcError[J, Err] =
    FromRpcError.instance {
      case RpcError(RpcErrorCode(1000), _, _) => Some(DivByZero)
      case _ => None
    }

  // 2. Define a function used to send a request and receive a response

  def send[F[_]](req: Json)(using MonadError[F, Throwable]): F[Option[String]] =
    CalcServer.handle[F](req.noSpaces).map(_.noSpaces).value

  // 3. Define helpers for building requests (optional)

  def add(a: Int, b: Int) =
    RpcRequest[(Int, Int), Int]("calc.add", (a, b))

  def mul(a: Int, b: Int) =
    RpcRequest[(Int, Int), Int]("calc.mul", (a, b))

  def div(a: Int, b: Int) =
    RpcRequest[(Int, Int), (Int, Int)]("calc.div", (a, b))

  // 4. Make some calls

  // a dummy function with an effect
  def printToConsole[F[_]: Applicative, A](a: A): F[Unit] =
    Applicative[F].pure(())

  def run[F[_]](using MonadError[F, Throwable]): EitherT[F, RpcCallError[Err], (Int, Int)] =
    call[F]((add(2, 3), mul(5, 8))).flatMap { (a, b) =>
      call[F](div(b, a)).flatMap { a =>
        liftF(printToConsole(a)) *> pure[F](a)
      }
    }.runWith[Err](send)
}
