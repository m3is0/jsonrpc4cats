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
package internal

import munit.FunSuite

import io.circe.Json
import io.circe.syntax.*

import jsonrpc4cats.*
import jsonrpc4cats.circe.given

class ErrorResponseTests extends FunSuite {

  test("Encode a response with a ServerError") {

    val res = ErrorResponse(InternalError, NumberId(1))
    val exp = """{"jsonrpc":"2.0","error":{"code":-32603,"message":"Internal error"},"id":1}"""

    assertEquals(
      JsonEncoder[Json, ErrorResponse[ServerError]].apply(res).noSpaces,
      exp
    )

  }

  test("Encode a response with an RpcError") {

    val err = RpcError(RpcErrorCode(1000), "App error", Some(Map("a" -> 0, "b" -> 1).asJson))
    val res = ErrorResponse(err, NumberId(1))
    val exp = """{"jsonrpc":"2.0","error":{"code":1000,"message":"App error","data":{"a":0,"b":1}},"id":1}"""

    assertEquals(
      JsonEncoder[Json, ErrorResponse[RpcError[Json]]].apply(res).noSpaces,
      exp
    )

  }

}
