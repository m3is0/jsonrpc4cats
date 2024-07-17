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

import munit.FunSuite

import io.circe.Json
import io.circe.syntax.*

import jsonrpc4cats.*
import jsonrpc4cats.circe.given

class RpcErrorTests extends FunSuite {

  test("Create a valid RpcErrorCode") {
    assert(RpcErrorCode(1000).toInt == 1000)
  }

  test("Create an invalid RpcErrorCode") {
    assert(compileErrors("RpcErrorCode(-32500)").contains("Invalid"))
  }

  test("Encode an RpcError with data") {
    val err = RpcError[Json](RpcErrorCode(1000), "App error", Some(Map("a" -> 0, "b" -> 1).asJson))
    val exp = """{"code":1000,"message":"App error","data":{"a":0,"b":1}}"""

    assertEquals(JsonEncoder[Json, RpcError[Json]].apply(err).noSpaces, exp)
  }

  test("Encode an RpcError without data") {
    val err = RpcError[Json](RpcErrorCode(1000), "App error", None)
    val exp = """{"code":1000,"message":"App error"}"""

    assertEquals(JsonEncoder[Json, RpcError[Json]].apply(err).noSpaces, exp)
  }

  test("Convert an RpcErr") {
    val err = RpcErr(RpcErrorCode(1000), "App error")
    val exp = RpcError[Json](RpcErrorCode(1000), "App error", None)

    assertEquals(ToRpcError[Json, RpcErr].apply(err), exp)
  }

}
