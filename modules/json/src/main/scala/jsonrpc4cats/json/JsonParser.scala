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

trait JsonParser[J] {
  def apply(s: String): Option[JsonParser.Result[J]]
}

object JsonParser {
  type Result[J] = Either[Vector[J], J]

  inline def apply[J](implicit instance: JsonParser[J]): JsonParser[J] =
    instance

  def instance[J](f: String => Option[Result[J]]): JsonParser[J] =
    new JsonParser[J] {
      def apply(s: String): Option[Result[J]] =
        f(s)
    }
}
