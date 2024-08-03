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

import cats.data.NonEmptyList

import jsonrpc4cats.FromRpcError
import jsonrpc4cats.RequestId
import jsonrpc4cats.RpcError
import jsonrpc4cats.RpcErrorCode

sealed trait RpcCallError[E]
final case class ResponseParseError[E](res: String) extends RpcCallError[E]
final case class ResponseDecodeError[E](res: String) extends RpcCallError[E]
final case class SendError[E](ex: Throwable) extends RpcCallError[E]

final case class ResponseErrors[E](toNel: NonEmptyList[ResponseError[E]]) extends RpcCallError[E]
object ResponseErrors {
  def unapply[E](e: ResponseErrors[E]): Option[(ResponseError[E], List[ResponseError[E]])] =
    Some((e.toNel.head, e.toNel.tail))
}

sealed trait ResponseError[E]
final case class ResponseNotFoundById[E](id: RequestId) extends ResponseError[E]
final case class ResultTypeDecodeError[E](id: RequestId) extends ResponseError[E]
final case class ServerError[E](code: Int, message: String, id: RequestId) extends ResponseError[E]
final case class UnknownError[E](code: Int, message: String, id: RequestId) extends ResponseError[E]
final case class DecodedError[E](err: E, id: RequestId) extends ResponseError[E]

object RpcCallError {
  def decode[J, E](err: RpcCallError[RpcError[Int, J]])(using
    fromRpcError: FromRpcError[J, E]
  ): RpcCallError[E] =
    err match {
      case errs: ResponseErrors[RpcError[Int, J]] =>
        ResponseErrors(
          errs.toNel.map {
            case DecodedError(e, id) =>
              RpcErrorCode.fromInt(e.code) match {
                case Some(c) =>
                  fromRpcError(RpcError(c, e.message, e.data)) match {
                    case Some(err) => DecodedError(err, id)
                    case None => UnknownError(c.toInt, e.message, id)
                  }
                case None => ServerError(e.code, e.message, id)
              }
            case err => err.asInstanceOf[ResponseError[E]]
          }
        )
      case err => err.asInstanceOf[RpcCallError[E]]
    }
}
