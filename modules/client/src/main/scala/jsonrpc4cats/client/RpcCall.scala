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

import cats.Applicative
import cats.ApplicativeError
import cats.Functor
import cats.data.EitherT
import cats.data.Kleisli
import cats.data.NonEmptyList
import cats.syntax.all.*

import jsonrpc4cats.FromRpcError
import jsonrpc4cats.Request
import jsonrpc4cats.RequestId
import jsonrpc4cats.RpcError
import jsonrpc4cats.client.internal.*
import jsonrpc4cats.json.JsonParser

object RpcCall {

  /**
   * A function used to send a request and receive a response
   */
  type Send[F[_], J] = J => F[Option[String]]

  /**
   * The RpcCall monad
   */
  type RpcCall[F[_], J, A] = Kleisli[[x] =>> EitherT[F, RpcCallError[RpcError[Int, J]], x], Send[F, J], A]

  extension [F[_]: Functor, J, A](call: RpcCall[F, J, A]) {

    /**
     * Runs RpcCall with the given 'Send' function and decodes errors to the specified type
     */
    def runWith[E](send: Send[F, J])(using FromRpcError[J, E]): EitherT[F, RpcCallError[E], A] =
      call.run(send).leftMap(RpcCallError.decode[J, E])
  }

  /**
   * Lifts a value into the context of RpcCall
   */
  def pure[F[_]]: PurePartiallyApplied[F] = new PurePartiallyApplied[F]

  private[client] final class PurePartiallyApplied[F[_]](val dummy: Boolean = true) {
    def apply[J, A](a: A)(using Applicative[F]): RpcCall[F, J, A] =
      Kleisli(_ => EitherT.pure(a))
  }

  /**
   * Lifts 'F[A]' into the context of RpcCall
   */
  def liftF[F[_]]: LiftFPartiallyApplied[F] = new LiftFPartiallyApplied[F]

  private[client] final class LiftFPartiallyApplied[F[_]](val dummy: Boolean = true) {
    def apply[J, A](fa: F[A])(using Functor[F]): RpcCall[F, J, A] =
      Kleisli(_ => EitherT.liftF(fa))
  }

  /**
   * Performs a call
   */
  def call[F[_]]: CallPartiallyApplied[F] = new CallPartiallyApplied[F]

  private[client] final class CallPartiallyApplied[F[_]](val dummy: Boolean = true) {

    /**
     * Performs a call with a single request
     */
    def apply[J, P, R](r: RpcRequest[P, R])(using
      ApplicativeError[F, Throwable],
      RequestEncoder[J],
      JsonParser[J],
      ResponseDecoder[J],
      RequestGenerator[J, Tuple1[RpcRequest[P, R]]],
      ResponseHandler[J, RequestGenerator.Signature[Tuple1[RpcRequest[P, R]]]]
    ): RpcCall[F, J, R] =
      apply(Tuple1(r)).map(_._1)

    /**
     * Performs a call with a single notification
     */
    def apply[J, P](r: RpcNotification[P])(using
      ApplicativeError[F, Throwable],
      RequestEncoder[J],
      JsonParser[J],
      ResponseDecoder[J],
      RequestGenerator[J, Tuple1[RpcNotification[P]]],
      ResponseHandler[J, RequestGenerator.Signature[Tuple1[RpcNotification[P]]]]
    ): RpcCall[F, J, Unit] =
      apply(Tuple1(r)).map(_ => ())

    /**
     * Performs a call with a request represented as a tuple
     *
     * @param t A tuple of RpcRequest or RpcNotification objects
     * @result A tuple with results for all RpcRequest objects, skipping RpcNotification's
     */
    def apply[J, T <: NonEmptyTuple](t: T)(using
      ApplicativeError[F, Throwable],
      RequestEncoder[J],
      JsonParser[J],
      ResponseDecoder[J]
    )(using
      generateRequest: RequestGenerator[J, T],
      handleResponse: ResponseHandler[J, RequestGenerator.Signature[T]]
    ): RpcCall[F, J, ResponseHandler.OutR[J, RequestGenerator.Signature[T]]] = {
      val (sig, req) = generateRequest(t)
      callU[F, J](req)
        .map(res => handleResponse(sig, res))
        .mapF(_.transform {
          case Right(Right(res)) =>
            Right(res)
          case Right(Left(err)) =>
            Left(ResponseErrors(err))
          case Left(err) =>
            Left(err)
        })
    }

  }

  /**
   * Performs an 'untyped' call with a request represented as a non-empty list
   */
  private[client] def callU[F[_], J](req: NonEmptyList[Request[J]])(using
    ApplicativeError[F, Throwable]
  )(using
    encodeRequest: RequestEncoder[J],
    parseResponse: JsonParser[J],
    decodeResponse: ResponseDecoder[J]
  ): RpcCall[F, J, Map[RequestId, Response[J]]] =
    Kleisli { send =>
      EitherT {
        send(encodeRequest(req)).attempt.map {
          case Right(Some(str)) =>
            parseResponse(str).toRight(ResponseParseError(str)).flatMap { res =>
              decodeResponse(res).toRight(ResponseDecodeError(str))
            }
          case Right(None) =>
            Right(Map())
          case Left(ex) =>
            Left(SendError(ex))
        }
      }
    }
}
