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

import cats.data.NonEmptyList

import jsonrpc4cats.Request
import jsonrpc4cats.json.JsonEncoder
import jsonrpc4cats.json.JsonFacade

trait RequestEncoder[J] {
  def apply(req: NonEmptyList[Request[J]]): J
}

object RequestEncoder {
  inline def apply[J](using instance: RequestEncoder[J]): RequestEncoder[J] =
    instance

  given requestEncoder[J](using
    json: JsonFacade[J],
    encodeRequest: JsonEncoder[J, Request[J]]
  ): RequestEncoder[J] with {
    def apply(req: NonEmptyList[Request[J]]): J =
      req match {
        case NonEmptyList(r, Nil) =>
          encodeRequest(r)
        case xs =>
          json.fromValues(xs.toList.map(encodeRequest(_)): _*)
      }
  }
}
