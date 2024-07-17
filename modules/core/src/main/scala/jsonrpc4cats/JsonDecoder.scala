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

trait JsonDecoder[J, A] {
  def apply(j: J): Option[A]
}

object JsonDecoder {
  inline def apply[J, A](using instance: JsonDecoder[J, A]): JsonDecoder[J, A] =
    instance

  inline def instance[J, A](f: J => Option[A]): JsonDecoder[J, A] =
    new JsonDecoder[J, A] {
      def apply(j: J): Option[A] =
        f(j)
    }

  given emptyTupleJsonDecoder[J](using json: JsonFacade[J]): JsonDecoder[J, EmptyTuple] =
    instance { j =>
      json.asVector(j) match {
        case Some(Vector()) =>
          Some(EmptyTuple)
        case _ =>
          None
      }
    }
}
