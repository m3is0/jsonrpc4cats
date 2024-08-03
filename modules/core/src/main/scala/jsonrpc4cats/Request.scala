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

final case class Request[P](method: String, params: P, id: Option[RequestId])

object Request {
  given jsonEncoder[J, E](using
    json: JsonFacade[J],
    encodeId: JsonEncoder[J, RequestId]
  ): JsonEncoder[J, Request[J]] =
    JsonEncoder.instance { v =>
      val fields = List(
        "jsonrpc" -> json.fromString(Protocol.Version),
        "method" -> json.fromString(v.method),
        "params" -> v.params
      )

      v.id match {
        case Some(id) =>
          json.fromFields(fields :+ ("id" -> encodeId(id)): _*)
        case None =>
          json.fromFields(fields: _*)
      }
    }

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
