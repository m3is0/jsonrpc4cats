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

import jsonrpc4cats.circe.given
import jsonrpc4cats.json.*

class RequestIdTests extends FunSuite {

  private def encdec(v: RequestId): Option[RequestId] =
    JsonDecoder[Json, RequestId]
      .apply(JsonEncoder[Json, RequestId].apply(v))

  test("StringId test") {
    assert(encdec(StringId("abc")) == Some(StringId("abc")))
  }

  test("NumberId test") {
    assert(encdec(NumberId(123)) == Some(NumberId(123)))
  }

  test("NullId test") {
    assert(encdec(NullId) == Some(NullId))
  }

}
