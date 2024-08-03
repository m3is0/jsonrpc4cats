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

import jsonrpc4cats.*
import jsonrpc4cats.circe.given

class ResponseHandlerTests extends FunSuite {

  import ResponseHandler.*

  // HandleResponse

  private def handleResponse[J, T <: Tuple](sig: T, res: Map[RequestId, Response[J]])(using
    handle: HandleResponse[J, T]
  ): HandleResponse.Out[J, T] =
    handle(sig, res)

  test("HandleResponse test") {

    val sig = (Const[RequestId, Int](NumberId(1)), Const[RequestId, Int](NumberId(2)))
    val res = Map[RequestId, Response[Json]](NumberId(2) -> Right(ResultResponse(10.asJson, NumberId(2))))

    assertEquals(
      handleResponse(sig, res),
      (Left(NonEmptyList.one(ResponseNotFoundById(NumberId(1)))), Right(10))
    )

  }

  test("HandleResponse with ResponseNotFoundById test") {

    val sig = Tuple1(Const[RequestId, Int](NumberId(1)))
    val res = Map.empty[RequestId, Response[Json]]

    assertEquals(
      handleResponse(sig, res),
      Tuple1(Left(NonEmptyList.one(ResponseNotFoundById(NumberId(1)))))
    )

  }

  test("HandleResponse with ResultTypeDecodeError test") {

    val sig = Tuple1(Const[RequestId, Int](NumberId(1)))
    val res = Map[RequestId, Response[Json]](NumberId(1) -> Right(ResultResponse("abc".asJson, NumberId(1))))

    assertEquals(
      handleResponse(sig, res),
      Tuple1(Left(NonEmptyList.one(ResultTypeDecodeError(NumberId(1)))))
    )

  }

  test("HandleResponse with DecodedError test") {

    val sig = Tuple1(Const[RequestId, Int](NumberId(1)))
    val res = Map[RequestId, Response[Json]](
      NumberId(1) -> Left(ErrorResponse(RpcError[Int, Json](1000, "error"), NumberId(1)))
    )

    assertEquals(
      handleResponse(sig, res),
      Tuple1(Left(NonEmptyList.one(DecodedError(RpcError[Int, Json](1000, "error"), NumberId(1)))))
    )

  }

  // Tupled

  private def tupled[E, T <: Tuple](t: T)(using
    tup: Tupled[E, T]
  ): Tupled.Out[E, T] =
    tup(t)

  test("Tupled with errors test") {

    type ErrOr[A] = Either[NonEmptyList[String], A]

    val tup: (ErrOr[Int], ErrOr[String], ErrOr[Boolean]) =
      (Right(1), Left(NonEmptyList.one("a")), Left(NonEmptyList.one("b")))

    val res = Left(NonEmptyList.of("a", "b"))

    assertEquals(tupled(tup), res)
  }

  test("Tupled without errors test") {

    type ErrOr[A] = Either[NonEmptyList[String], A]

    val tup: (ErrOr[Int], ErrOr[String], ErrOr[Boolean]) =
      (Right(1), Right("a"), Right(true))

    val res = Right((1, "a", true))

    assertEquals(tupled(tup), res)
  }

  // ResponseHandler

  private def responseHandler[J, T <: Tuple](sig: T, res: Map[RequestId, Response[J]])(using
    handle: ResponseHandler[J, T]
  ): ResponseHandler.Out[J, T] =
    handle(sig, res)

  test("ResponseHandler test") {

    val sig = (Const[RequestId, Int](NumberId(1)), Const[RequestId, Int](NumberId(2)))
    val res = Map[RequestId, Response[Json]](
      NumberId(1) -> Right(ResultResponse(5.asJson, NumberId(1))),
      NumberId(2) -> Right(ResultResponse(8.asJson, NumberId(2)))
    )

    assertEquals(
      responseHandler(sig, res),
      Right((5, 8))
    )
  }

}
