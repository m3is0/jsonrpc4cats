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

sealed trait RequestId extends Product with Serializable
final case class StringId(value: String) extends RequestId
final case class NumberId(value: Int) extends RequestId
case object NullId extends RequestId

object RequestId {
  given jsonEncoder[J](using json: JsonFacade[J]): JsonEncoder[J, RequestId] =
    JsonEncoder.instance {
      case StringId(s) => json.fromString(s)
      case NumberId(i) => json.fromInt(i)
      case _ => json.nullValue
    }

  given jsonDecoder[J](using json: JsonFacade[J]): JsonDecoder[J, RequestId] =
    JsonDecoder.instance { v =>
      json
        .asString(v)
        .map(StringId.apply)
        .orElse(json.asInt(v).map(NumberId.apply))
        .orElse(Some(NullId))
    }
}
