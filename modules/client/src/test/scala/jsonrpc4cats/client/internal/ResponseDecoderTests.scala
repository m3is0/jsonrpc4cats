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

package jsonrpc4cats.client.internal

import munit.FunSuite

import cats.syntax.all.*
import io.circe.Json
import io.circe.parser.parse

import jsonrpc4cats.NumberId
import jsonrpc4cats.circe.given

class ResponseDecoderTests extends FunSuite {

  test("Decode a single response") {

    val r = """{"jsonrpc":"2.0","result":7,"id":1}"""

    val rsp: Option[Either[Vector[Json], Json]] =
      parse(r).map(Right(_)).toOption

    val res = rsp.flatMap(ResponseDecoder[Json].apply)

    assertEquals(res.map(_.get(NumberId(1)).isDefined), Some(true))
  }

  test("Decode a batch response") {

    val r1 = """{"jsonrpc":"2.0","result":7,"id":1}"""
    val r2 = """{"jsonrpc":"2.0","error":{"code":1000,"message":"error"},"id":2}"""

    val rsp: Option[Either[Vector[Json], Json]] =
      Vector(r1, r2).traverse(parse).map(Left(_)).toOption

    val res = rsp.flatMap(ResponseDecoder[Json].apply)

    assertEquals(res.map(_.get(NumberId(1)).isDefined), Some(true))
    assertEquals(res.map(_.get(NumberId(2)).isDefined), Some(true))
  }

}
