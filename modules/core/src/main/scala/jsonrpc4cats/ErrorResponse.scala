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

import jsonrpc4cats.json.JsonDecoder
import jsonrpc4cats.json.JsonEncoder
import jsonrpc4cats.json.JsonFacade

final case class ErrorResponse[+E](error: E, id: RequestId)

object ErrorResponse {
  given jsonEncoder[J, E](using
    json: JsonFacade[J],
    encodeError: JsonEncoder[J, E],
    encodeId: JsonEncoder[J, RequestId]
  ): JsonEncoder[J, ErrorResponse[E]] =
    JsonEncoder.instance { v =>
      json.fromFields(
        "jsonrpc" -> json.fromString(Protocol.Version),
        "error" -> encodeError(v.error),
        "id" -> encodeId(v.id)
      )
    }

  given jsonDecoder[J, E](using
    json: JsonFacade[J],
    decodeError: JsonDecoder[J, E],
    decodeId: JsonDecoder[J, RequestId]
  ): JsonDecoder[J, ErrorResponse[E]] =
    JsonDecoder.instance { j =>
      json.asMap(j).flatMap { o =>
        for {
          _ <- o.get("jsonrpc").flatMap(json.asString).filter(_ == Protocol.Version)
          e <- o.get("error").flatMap(decodeError.apply)
          i <- o.get("id").flatMap(decodeId.apply)
        } yield ErrorResponse(e, i)
      }
    }
}
