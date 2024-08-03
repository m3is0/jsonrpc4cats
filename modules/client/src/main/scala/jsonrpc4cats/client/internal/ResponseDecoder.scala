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

import cats.syntax.all.*

import jsonrpc4cats.ErrorResponse
import jsonrpc4cats.RequestId
import jsonrpc4cats.ResultResponse
import jsonrpc4cats.RpcError
import jsonrpc4cats.json.JsonDecoder

trait ResponseDecoder[J] {
  def apply(res: Either[Vector[J], J]): Option[Map[RequestId, Response[J]]]
}

object ResponseDecoder {
  inline def apply[J](using instance: ResponseDecoder[J]): ResponseDecoder[J] =
    instance

  given responseDecoder[J](using
    decodeError: JsonDecoder[J, ErrorResponse[RpcError[Int, J]]],
    decodeResult: JsonDecoder[J, ResultResponse[J]]
  ): ResponseDecoder[J] with {
    def apply(res: Either[Vector[J], J]): Option[Map[RequestId, Response[J]]] =
      res
        .fold(identity, Vector(_))
        .traverse(j => decodeResult(j).map(Right(_)).orElse(decodeError(j).map(Left(_))))
        .map(_.map(r => (r.fold(_.id, _.id), r)).toMap)
  }
}
