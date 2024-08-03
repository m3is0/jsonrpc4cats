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

package jsonrpc4cats

import munit.FunSuite

import io.circe.Json
import io.circe.parser.parse
import io.circe.syntax.*

import jsonrpc4cats.circe.given
import jsonrpc4cats.json.*

class RpcErrorTests extends FunSuite {

  test("Create a valid RpcErrorCode") {
    assertEquals(RpcErrorCode(1000).toInt, 1000)
    assertEquals(RpcErrorCode.fromInt(1000).map(_.toInt), Some(1000))
  }

  test("Create an invalid RpcErrorCode") {
    assert(compileErrors("RpcErrorCode(-32500)").contains("Invalid"))
    assertEquals(RpcErrorCode.fromInt(-32500), None)
  }

  test("Encode an RpcError with a JSON payload") {
    val err = RpcError[RpcErrorCode, Json](RpcErrorCode(1000), "App error", Some(Map("a" -> 0, "b" -> 1).asJson))
    val exp = """{"code":1000,"message":"App error","data":{"a":0,"b":1}}"""

    assertEquals(JsonEncoder[Json, RpcError[RpcErrorCode, Json]].apply(err).noSpaces, exp)
  }

  test("Encode an RpcError without a JSON payload") {
    val err = RpcError[RpcErrorCode, Json](RpcErrorCode(1000), "App error", None)
    val exp = """{"code":1000,"message":"App error"}"""

    assertEquals(JsonEncoder[Json, RpcError[RpcErrorCode, Json]].apply(err).noSpaces, exp)
  }

  test("Decode an RpcError with a JSON payload") {
    val err = """{"code":1000,"message":"App error","data":{"a":0,"b":1}}"""
    val exp = RpcError[Int, Json](1000, "App error", Some(Map("a" -> 0, "b" -> 1).asJson))

    assertEquals(
      parse(err).toOption.flatMap(JsonDecoder[Json, RpcError[Int, Json]].apply),
      Some(exp)
    )
  }

  test("Decode an RpcError without a JSON payload") {
    val err = """{"code":1000,"message":"App error"}"""
    val exp = RpcError[Int, Json](1000, "App error", None)

    assertEquals(
      parse(err).toOption.flatMap(JsonDecoder[Json, RpcError[Int, Json]].apply),
      Some(exp)
    )
  }

}
