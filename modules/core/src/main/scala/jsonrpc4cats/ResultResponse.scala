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

final case class ResultResponse[+R](result: R, id: RequestId)

object ResultResponse {
  given jsonEncoder[J, R](using
    json: JsonFacade[J],
    encodeResult: JsonEncoder[J, R],
    encodeId: JsonEncoder[J, RequestId]
  ): JsonEncoder[J, ResultResponse[R]] =
    JsonEncoder.instance { v =>
      json.fromFields(
        "jsonrpc" -> json.fromString(Protocol.Version),
        "result" -> encodeResult(v.result),
        "id" -> encodeId(v.id)
      )
    }

  given jsonDecoder[J, R](using
    json: JsonFacade[J],
    decodeResult: JsonDecoder[J, R],
    decodeId: JsonDecoder[J, RequestId]
  ): JsonDecoder[J, ResultResponse[R]] =
    JsonDecoder.instance { j =>
      json.asMap(j).flatMap { o =>
        for {
          _ <- o.get("jsonrpc").flatMap(json.asString).filter(_ == Protocol.Version)
          r <- o.get("result").flatMap(decodeResult.apply)
          i <- o.get("id").flatMap(decodeId.apply)
        } yield ResultResponse(r, i)
      }
    }
}
