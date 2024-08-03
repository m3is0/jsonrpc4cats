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

trait JsonFacade[J] {
  def asString(j: J): Option[String]
  def asInt(j: J): Option[Int]
  def asMap(j: J): Option[Map[String, J]]
  def asVector(j: J): Option[Vector[J]]
  def fromString(s: String): J
  def fromInt(i: Int): J
  def fromFields(fs: (String, J)*): J
  def fromValues(vs: J*): J
  def nullValue: J
}

object JsonFacade {
  inline def apply[J](using instance: JsonFacade[J]): JsonFacade[J] =
    instance
}
