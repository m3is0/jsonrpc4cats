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

import cats.Semigroup
import cats.data.Const
import cats.data.NonEmptyList
import cats.syntax.all.*

import jsonrpc4cats.RequestId
import jsonrpc4cats.RpcError
import jsonrpc4cats.json.JsonDecoder

trait ResponseHandler[J, T <: Tuple] {

  /**
   * Handles a response by a request signature
   *
   * @param sig A request signature as (Const[RequestId, R1], Const[RequestId, R2], ...)
   * @param res A map of responses and their RequestId's
   * @return A result as Either[ResponseHandler.ErrorNel[J], (R1, R2, ...)]
   */
  def apply(sig: T, res: Map[RequestId, Response[J]]): ResponseHandler.Out[J, T]
}

object ResponseHandler {

  type ErrorNel[J] = NonEmptyList[ResponseError[RpcError[Int, J]]]

  type Out[J, T <: Tuple] = Either[ErrorNel[J], OutR[J, T]]

  type OutR[J, T <: Tuple] = Tupled.OutR[ErrorNel[J], HandleResponse.Out[J, T]]

  given responseHandler[J, T <: Tuple](using
    handleResponse: HandleResponse[J, T],
    tupled: Tupled[ErrorNel[J], HandleResponse.Out[J, T]]
  ): ResponseHandler[J, T] with {
    def apply(sig: T, res: Map[RequestId, Response[J]]) =
      tupled(handleResponse(sig, res))
  }

  trait HandleResponse[J, T <: Tuple] {

    /**
     * Handles a response by a request signature and returns a tuple of results
     *
     * @param sig A request signature as (Const[RequestId, R1], Const[RequestId, R2], ...)
     * @param res A map of responses and their RequestId's
     * @return A tuple of results as (Either[ErrorNel[J], R1], Either[ErrorNel[J], R2], ...)
     */
    def apply(sig: T, res: Map[RequestId, Response[J]]): HandleResponse.Out[J, T]
  }

  object HandleResponse {

    type Out[J, T <: Tuple] <: Tuple =
      T match {
        case EmptyTuple => EmptyTuple
        case Const[RequestId, r] *: t => Either[ErrorNel[J], r] *: Out[J, t]
      }

    given emtyTupleHandleResponse[J]: HandleResponse[J, EmptyTuple] with {
      def apply(sig: EmptyTuple, res: Map[RequestId, Response[J]]) =
        EmptyTuple
    }

    given tupleHandleResponse[J, R, T <: Tuple](using
      tHandler: HandleResponse[J, T],
      decodeResult: JsonDecoder[J, R]
    ): HandleResponse[J, Const[RequestId, R] *: T] with {
      def apply(sig: Const[RequestId, R] *: T, res: Map[RequestId, Response[J]]) =
        sig match {
          case Const(reqId) *: t =>
            res.get(reqId) match {
              case Some(Left(e)) =>
                Left(NonEmptyList.one(DecodedError(e.error, reqId))) *: tHandler(t, res)
              case Some(Right(r)) =>
                decodeResult(r.result).toRight(NonEmptyList.one(ResultTypeDecodeError(reqId))) *: tHandler(t, res)
              case None =>
                Left(NonEmptyList.one(ResponseNotFoundById(reqId))) *: tHandler(t, res)
            }
        }
    }

  }

  trait Tupled[E, T <: Tuple] {

    /**
     * Processes a tuple of results in the same way as '.parTupled' do
     *
     * @param t A tuple of results as (Either[E, R1], Either[E, R2], ...)
     * @return A result as Either[E, (R1, R2, ...)]
     */
    def apply(t: T): Tupled.Out[E, T]
  }

  object Tupled {

    type Out[E, T <: Tuple] = Either[E, OutR[E, T]]

    type OutR[E, T <: Tuple] <: Tuple =
      T match {
        case EmptyTuple => EmptyTuple
        case Either[E, r] *: t => r *: OutR[E, t]
      }

    given emptyTuple[E]: Tupled[E, EmptyTuple] with {
      def apply(t: EmptyTuple) =
        Right(EmptyTuple)
    }

    given headTail[E: Semigroup, R, T <: Tuple](using
      tTupled: Tupled[E, T]
    ): Tupled[E, Either[E, R] *: T] with {
      def apply(t: Either[E, R] *: T) =
        t match {
          case h *: t =>
            (h, tTupled(t)) match {
              case (Right(a), Right(b)) => Right(a *: b)
              case (Right(a), Left(b)) => Left(b)
              case (Left(a), Right(b)) => Left(a)
              case (Left(a), Left(b)) => Left(a.combine(b))
            }
        }
    }

  }

}
