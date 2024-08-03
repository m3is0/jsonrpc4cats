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

import cats.data.NonEmptyList
import io.circe.Json
import io.circe.syntax.*

import jsonrpc4cats.NumberId
import jsonrpc4cats.Request
import jsonrpc4cats.circe.given

class RequestEncoderTests extends FunSuite {

  test("Encode a single request") {

    val req = NonEmptyList.one(
      Request("abc", (1, 2).asJson, Some(NumberId(1)))
    )

    assert(RequestEncoder[Json](req).isArray == false)
  }

  test("Encode a batch request") {

    val req = NonEmptyList.of(
      Request("abc", (1, 2).asJson, Some(NumberId(1))),
      Request("abc", (1, 2).asJson, Some(NumberId(2)))
    )

    assert(RequestEncoder[Json](req).isArray == true)
  }

}
