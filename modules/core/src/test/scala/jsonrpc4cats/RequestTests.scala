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

class RequestTests extends FunSuite {

  private def encode(req: Request[Json]): String =
    JsonEncoder[Json, Request[Json]].apply(req).noSpaces

  private def decode(req: String): Option[Request[Json]] =
    parse(req).toOption.flatMap(JsonDecoder[Json, Request[Json]].apply)

  test("Encode a request") {
    val req = Request("sum", (42, 23).asJson, Some(NumberId(1)))
    val exp = """{"jsonrpc":"2.0","method":"sum","params":[42,23],"id":1}"""

    assertEquals(encode(req), exp)
  }

  test("Encode a notification") {
    val req = Request("sum", (42, 23).asJson, None)
    val exp = """{"jsonrpc":"2.0","method":"sum","params":[42,23]}"""

    assertEquals(encode(req), exp)
  }

  test("Decode a request with params") {
    val req = """{"jsonrpc":"2.0","method":"sum","params":[42,23],"id":1}"""
    val exp = Request("sum", (42, 23).asJson, Some(NumberId(1)))

    assertEquals(decode(req), Some(exp))
  }

  test("Decode a request without params") {
    val req = """{"jsonrpc":"2.0","method":"foobar", "id":1}"""
    val exp = Request("foobar", JsonEncoder[Json, EmptyTuple].apply(EmptyTuple), Some(NumberId(1)))

    assertEquals(decode(req), Some(exp))
  }

  test("Decode a notification") {
    val req = """{"jsonrpc":"2.0","method":"update","params":[42, 23]}"""
    val exp = Request("update", (42, 23).asJson, None)

    assertEquals(decode(req), Some(exp))
  }

  test("Decode a request with an invalid protocol version") {
    val req = """{"jsonrpc":"1.0","method":"sum","params":[42, 23],"id":1}"""

    assertEquals(decode(req), None)
  }

  test("Decode a request without a protocol version") {
    val req = """{"method":"sum","params":[42, 23],"id":1}"""

    assertEquals(decode(req), None)
  }

}
