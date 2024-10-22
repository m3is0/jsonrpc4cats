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

import munit.FunSuite

import io.circe.Json

import jsonrpc4cats.circe.given

class HelloServerTests extends FunSuite {

  type Eff[A] = Either[Throwable, A]

  test("hello.hello") {
    val req = """{"jsonrpc":"2.0","method":"hello.hello","id":1}"""
    val exp = """{"jsonrpc":"2.0","result":"Hello!","id":1}"""

    val res = HelloServer.api[Eff].handle[Json](req)

    assertEquals(res.map(_.noSpaces).value, Right(Some(exp)))
  }

  test("hello.name") {
    val req = """{"jsonrpc":"2.0","method":"hello.name","params":["Jack"],"id":1}"""
    val exp = """{"jsonrpc":"2.0","result":"Hello, Jack!","id":1}"""

    val res = HelloServer.api[Eff].handle[Json](req)

    assertEquals(res.map(_.noSpaces).value, Right(Some(exp)))
  }

}
