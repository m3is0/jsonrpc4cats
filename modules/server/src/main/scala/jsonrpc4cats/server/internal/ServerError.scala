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

import jsonrpc4cats.json.JsonEncoder
import jsonrpc4cats.json.JsonFacade

sealed trait ServerError(val code: Int, val message: String)
case object ParseError extends ServerError(-32700, "Parse error")
case object InvalidRequest extends ServerError(-32600, "Invalid Request")
case object MethodNotFound extends ServerError(-32601, "Method not found")
case object InvalidParams extends ServerError(-32602, "Invalid params")
case object InternalError extends ServerError(-32603, "Internal error")

object ServerError {
  given jsonEncoder[J](using json: JsonFacade[J]): JsonEncoder[J, ServerError] =
    JsonEncoder.instance { v =>
      json.fromFields(
        "code" -> json.fromInt(v.code),
        "message" -> json.fromString(v.message)
      )
    }
}
