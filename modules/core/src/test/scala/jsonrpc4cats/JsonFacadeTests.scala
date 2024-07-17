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

abstract class JsonFacadeTests[J](using json: JsonFacade[J]) extends FunSuite {

  test("String test") {
    val res = json.asString(json.fromString("abc"))
    assert(res.isDefined)
  }

  test("Int test") {
    val res = json.asInt(json.fromInt(123))
    assert(res.isDefined)
  }

  test("Map test") {
    val res = json.asMap(json.fromFields("a" -> json.fromString("abc"), "b" -> json.fromInt(123)))
    assert(res.isDefined)
  }

  test("Seq test") {
    val res = json.asVector(json.fromValues(json.fromString("abc"), json.fromInt(123)))
    assert(res.isDefined)
  }
}
