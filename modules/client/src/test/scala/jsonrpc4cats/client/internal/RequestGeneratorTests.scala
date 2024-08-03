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

package jsonrpc4cats.client
package internal

import munit.FunSuite

import cats.data.Const
import cats.data.NonEmptyList
import io.circe.Json
import io.circe.syntax.*

import jsonrpc4cats.NumberId
import jsonrpc4cats.Request
import jsonrpc4cats.RequestId
import jsonrpc4cats.circe.given

class RequestGeneratorTests extends FunSuite {

  import RequestGenerator.*

  private def zipWithIndex[T <: NonEmptyTuple](t: T)(using
    zwi: ZipWithIndex[T]
  ): ZipWithIndex.Out[T] =
    zwi(t, 1)

  private def generateSignature[T <: Tuple](t: T)(using
    genIds: GenerateSignature[T]
  ): GenerateSignature.Out[T] =
    genIds(t)

  private def generateRequest[J, T <: NonEmptyTuple](t: T)(using
    genReq: GenerateRequest[J, T]
  ): NonEmptyList[Request[J]] =
    genReq(t)

  private def generateAll[J, T <: NonEmptyTuple](t: T)(using
    genReq: RequestGenerator[J, T]
  ): Out[J, T] =
    genReq(t)

  test("ZipWithIndex test") {
    assertEquals(
      zipWithIndex((123, "abc", true)),
      ((1, 123), (2, "abc"), (3, true))
    )
  }

  test("GenerateSignature test") {
    val req = (
      RpcRequest[(Int, Int), Int]("add", (1, 2)),
      RpcNotification[(Int, Int)]("put", (1, 2)),
      RpcRequest[(Int, Int), (Int, Int)]("div", (1, 2))
    )

    assertEquals(
      generateSignature(zipWithIndex(req)),
      (Const[RequestId, Int](NumberId(1)), Const[RequestId, (Int, Int)](NumberId(3)))
    )
  }

  test("GenerateRequest test") {
    val req = (
      RpcRequest[(Int, Int), Int]("add", (1, 2)),
      RpcNotification[(Int, Int)]("put", (1, 2)),
      RpcRequest[(Int, Int), (Int, Int)]("div", (1, 2))
    )

    val exp = NonEmptyList.of(
      Request("add", (1, 2).asJson, Some(NumberId(1))),
      Request("put", (1, 2).asJson, None),
      Request("div", (1, 2).asJson, Some(NumberId(3)))
    )

    assertEquals(
      generateRequest(zipWithIndex(req)),
      exp
    )
  }

  test("RequestGenerator test") {
    val req = (
      RpcRequest[(Int, Int), Int]("add", (1, 2)),
      RpcNotification[(Int, Int)]("put", (1, 2)),
      RpcRequest[(Int, Int), (Int, Int)]("div", (1, 2))
    )

    val ids = (
      Const[RequestId, Int](NumberId(1)),
      Const[RequestId, (Int, Int)](NumberId(3))
    )

    val rqs = NonEmptyList.of(
      Request("add", (1, 2).asJson, Some(NumberId(1))),
      Request("put", (1, 2).asJson, None),
      Request("div", (1, 2).asJson, Some(NumberId(3)))
    )

    val exp = (ids, rqs)

    assertEquals(
      generateAll(req),
      exp
    )
  }

}
