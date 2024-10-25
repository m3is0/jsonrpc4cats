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
import cats.MonadError
import cats.data.OptionT
import io.circe.Json

import jsonrpc4cats.*
import jsonrpc4cats.circe.given
import jsonrpc4cats.server.*

object CalcServer {

  // 1. Define errors

  sealed trait Err
  case object DivByZero extends Err

  given errToRpcError[J]: ToRpcError[J, Err] =
    ToRpcError.instance {
      case DivByZero =>
        RpcError(RpcErrorCode(1000), "Division by zero")
    }

  // 2. Define methods

  def add[F[_]](using F: Applicative[F]) =
    RpcMethod.instance[F, "calc.add", (Int, Int), Err, Long] { (a, b) =>
      F.pure(Right(a.toLong + b.toLong))
    }

  def subtract[F[_]](using F: Applicative[F]) =
    RpcMethod.instance[F, "calc.sub", (Int, Int), Err, Long] { (a, b) =>
      F.pure(Right(a.toLong - b.toLong))
    }

  def multiply[F[_]](using F: Applicative[F]) =
    RpcMethod.instance[F, "calc.mul", (Int, Int), Err, Long] { (a, b) =>
      F.pure(Right(a.toLong * b.toLong))
    }

  def divide[F[_]](using F: Applicative[F]) =
    RpcMethod.instance[F, "calc.div", (Int, Int), Err, (Int, Int)] {
      case (_, 0) =>
        F.pure(Left(DivByZero))
      case (a, b) =>
        F.pure(Right((a / b, a % b)))
    }

  // 3. Define API (uniqueness of method names is checked at compile time)

  def api[F[_]: Applicative] =
    RpcServer
      .add(add[F])
      .add(subtract[F])
      .add(multiply[F])
      .add(divide[F])

  // 4. Handle a request (you can use any JSON library for which a module exists)

  def handle[F[_]](req: String)(using MonadError[F, Throwable]): OptionT[F, Json] =
    api[F].handle[Json](req)
}
