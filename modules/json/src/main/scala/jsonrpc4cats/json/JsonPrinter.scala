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

package jsonrpc4cats.json

trait JsonPrinter[J] {
  def apply(j: J): String
}

object JsonPrinter {
  inline def apply[J](implicit instance: JsonPrinter[J]): JsonPrinter[J] =
    instance

  def instance[J](f: J => String): JsonPrinter[J] =
    new JsonPrinter[J] {
      def apply(j: J): String =
        f(j)
    }
}
