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

class ErrorResponseTests extends FunSuite {

  test("Encode a response") {
    val res = ErrorResponse(Map("code" -> "123", "message" -> "error"), NumberId(1))
    val exp = """{"jsonrpc":"2.0","error":{"code":"123","message":"error"},"id":1}"""

    assertEquals(
      JsonEncoder[Json, ErrorResponse[Map[String, String]]].apply(res).noSpaces,
      exp
    )
  }

  test("Decode a response") {
    val res = """{"jsonrpc":"2.0","error":{"code":"123","message":"error"},"id":1}"""
    val exp = ErrorResponse(Map("code" -> "123", "message" -> "error").asJson, NumberId(1))

    assertEquals(
      parse(res).toOption.flatMap(JsonDecoder[Json, ErrorResponse[Json]].apply),
      Some(exp)
    )
  }

}
