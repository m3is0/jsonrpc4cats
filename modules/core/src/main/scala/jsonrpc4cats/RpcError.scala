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

import scala.compiletime.error

import jsonrpc4cats.json.JsonDecoder
import jsonrpc4cats.json.JsonEncoder
import jsonrpc4cats.json.JsonFacade

opaque type RpcErrorCode = Int

object RpcErrorCode {
  transparent inline def apply(i: Int): RpcErrorCode =
    inline if i < -32768 || i > -32000 then i.asInstanceOf[RpcErrorCode]
    else error("Invalid RpcErrorCode")

  def fromInt(i: Int): Option[RpcErrorCode] =
    if i < -32768 || i > -32000 then Some(i) else None

  extension (c: RpcErrorCode) {
    def toInt: Int = c
  }

  def unapply(c: RpcErrorCode): Option[Int] =
    Some(c)
}

final case class RpcError[C, J](code: C, message: String, data: Option[J] = None)

object RpcError {
  given jsonEncoder[J](using
    json: JsonFacade[J]
  ): JsonEncoder[J, RpcError[RpcErrorCode, J]] =
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

  given jsonDecoder[J](using
    json: JsonFacade[J]
  ): JsonDecoder[J, RpcError[Int, J]] =
    JsonDecoder.instance { j =>
      json.asMap(j).flatMap { o =>
        for {
          c <- o.get("code").flatMap(json.asInt)
          m <- o.get("message").flatMap(json.asString)
          d <- Some(o.get("data"))
        } yield RpcError(c, m, d)
      }
    }
}

trait ToRpcError[J, E] {
  def apply(e: E): RpcError[RpcErrorCode, J]
}

object ToRpcError {
  inline def apply[J, E](using instance: ToRpcError[J, E]): ToRpcError[J, E] =
    instance

  inline def instance[J, E](f: E => RpcError[RpcErrorCode, J]): ToRpcError[J, E] =
    new ToRpcError[J, E] {
      def apply(e: E): RpcError[RpcErrorCode, J] =
        f(e)
    }
}

trait FromRpcError[J, E] {
  def apply(e: RpcError[RpcErrorCode, J]): Option[E]
}

object FromRpcError {
  inline def instance[J, E](f: RpcError[RpcErrorCode, J] => Option[E]): FromRpcError[J, E] =
    new FromRpcError[J, E] {
      def apply(e: RpcError[RpcErrorCode, J]): Option[E] =
        f(e)
    }
}
