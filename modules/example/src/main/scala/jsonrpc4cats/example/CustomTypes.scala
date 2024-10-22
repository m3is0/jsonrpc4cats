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
import io.circe.Decoder
import io.circe.Encoder
import io.circe.Json
import io.circe.generic.semiauto.*
import io.circe.syntax.*

import jsonrpc4cats.*
import jsonrpc4cats.circe.given
import jsonrpc4cats.server.*

object CustomTypes {

  sealed trait DivError
  final case class DivByZero(divident: Int) extends DivError

  given toRpcError: ToRpcError[Json, DivError] =
    ToRpcError.instance { case DivByZero(a) =>
      RpcError(RpcErrorCode(1000), "Division by zero", Some(Map("divident" -> a).asJson))
    }

  final case class DivParams(divident: Int, divisor: Int)
  given paramsDecoder: Decoder[DivParams] = deriveDecoder[DivParams]

  final case class DivResult(quotient: Int, remainder: Int)
  given resultEncoder: Encoder[DivResult] = deriveEncoder[DivResult]

  def div[F[_]](using F: Applicative[F]) =
    RpcMethod.instance[F, "custom.div", DivParams, DivError, DivResult] {
      case DivParams(a, 0) =>
        F.pure(Left(DivByZero(a)))
      case DivParams(a, b) =>
        F.pure(Right(DivResult(a / b, a % b)))
    }

  def handle[F[_]](req: String)(using MonadError[F, Throwable]): OptionT[F, Json] =
    RpcServer.add(div[F]).handle[Json](req)
}
