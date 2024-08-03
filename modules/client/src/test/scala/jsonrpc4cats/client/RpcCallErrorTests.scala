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

package jsonrpc4cats.client

import munit.FunSuite

import cats.data.NonEmptyList
import io.circe.Json

import jsonrpc4cats.*

class RpcCallErrorTests extends FunSuite {

  sealed trait Err
  case object DivByZero extends Err

  object Err {
    given errFromRpcError[J]: FromRpcError[J, Err] =
      FromRpcError.instance {
        case RpcError(RpcErrorCode(1000), _, _) => Some(DivByZero)
        case _ => None
      }
  }

  test("RpcCallError.decode test") {

    val err: RpcCallError[RpcError[Int, Json]] = ResponseErrors(
      NonEmptyList.one(
        DecodedError(RpcError(1000, "Division by zero", None), NumberId(1))
      )
    )

    val exp: RpcCallError[Err] = ResponseErrors(
      NonEmptyList.one(
        DecodedError(DivByZero, NumberId(1))
      )
    )

    assertEquals(
      RpcCallError.decode[Json, Err](err),
      exp
    )

  }

  test("ResponseErrors.unapply test") {

    val err: RpcCallError[Err] = ResponseErrors(
      NonEmptyList.one(
        DecodedError(DivByZero, NumberId(1))
      )
    )

    assert {
      err match {
        case ResponseErrors(DecodedError(DivByZero, _), _) => true
        case _ => false
      }
    }

  }

}
