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

package jsonrpc4cats.server.internal

import jsonrpc4cats.JsonDecoder
import jsonrpc4cats.JsonEncoder
import jsonrpc4cats.JsonFacade

final case class Request[P](method: String, params: P, id: Option[RequestId])

object Request {
  given jsonDecoder[J](using
      json: JsonFacade[J],
      encodeParams: JsonEncoder[J, EmptyTuple],
      decodeId: JsonDecoder[J, RequestId]
  ): JsonDecoder[J, Request[J]] =
    JsonDecoder.instance { v =>
      json.asMap(v).flatMap { obj =>
        for {
          _ <- obj.get("jsonrpc").flatMap(json.asString).filter(_ == Protocol.Version)
          m <- obj.get("method").flatMap(json.asString)
          p <- Some(obj.get("params").getOrElse(encodeParams(EmptyTuple)))
          i <- Some(obj.get("id").flatMap(decodeId.apply))
        } yield Request(m, p, i)
      }
    }
}
