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

trait JsonEncoder[J, A] {
  def apply(a: A): J
}

object JsonEncoder {
  inline def apply[J, A](using instance: JsonEncoder[J, A]): JsonEncoder[J, A] =
    instance

  inline def instance[J, A](f: A => J): JsonEncoder[J, A] =
    new JsonEncoder[J, A] {
      def apply(a: A): J =
        f(a)
    }

  given emptyTupleEncoder[J](using json: JsonFacade[J]): JsonEncoder[J, EmptyTuple] =
    instance { _ =>
      json.fromValues()
    }
}
