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

package jsonrpc4cats.server

import scala.compiletime.error

import jsonrpc4cats.JsonEncoder
import jsonrpc4cats.JsonFacade

opaque type RpcErrorCode = Int

object RpcErrorCode {
  transparent inline def apply(i: Int): RpcErrorCode =
    inline if i < -32768 || i > -32000 then i.asInstanceOf[RpcErrorCode]
    else error("Invalid RpcErrorCode")

  extension (c: RpcErrorCode) {
    def toInt: Int = c
  }
}

final case class RpcError[J](code: RpcErrorCode, message: String, data: Option[J] = None)

object RpcError {
  given jsonEncoder[J](using json: JsonFacade[J]): JsonEncoder[J, RpcError[J]] =
    JsonEncoder.instance {
      case RpcError(code, msg, Some(data)) =>
        json.fromFields(
          "code" -> json.fromInt(code.toInt),
          "message" -> json.fromString(msg),
          "data" -> data
        )
      case RpcError(code, msg, None) =>
        json.fromFields(
          "code" -> json.fromInt(code.toInt),
          "message" -> json.fromString(msg)
        )

    }
}

trait ToRpcError[J, E] {
  def apply(e: E): RpcError[J]
}

object ToRpcError {
  inline def apply[J, E](using instance: ToRpcError[J, E]): ToRpcError[J, E] =
    instance

  inline def instance[J, E](f: E => RpcError[J]): ToRpcError[J, E] =
    new ToRpcError[J, E] {
      def apply(e: E): RpcError[J] =
        f(e)
    }
}

final case class RpcErr(code: RpcErrorCode, message: String) extends Product with Serializable

object RpcErr {
  given toRpcError[J]: ToRpcError[J, RpcErr] =
    ToRpcError.instance { e =>
      RpcError(e.code, e.message)
    }
}

final case class RpcErrorInfo[J](
    code: Int,
    message: String,
    data: Option[J],
    request: String,
    exception: Option[Throwable]
)
