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

package jsonrpc4cats.circe

import io.circe.Decoder
import io.circe.Encoder
import io.circe.Json
import io.circe.parser.parse

import jsonrpc4cats.JsonDecoder
import jsonrpc4cats.JsonEncoder
import jsonrpc4cats.JsonFacade
import jsonrpc4cats.JsonParser

given jsonParser: JsonParser[Json] =
  JsonParser.instance[Json] { s =>
    parse(s).fold(
      _ => None,
      _.arrayOrObject(
        None,
        arr => Some(Left(arr)),
        obj => Some(Right(Json.fromJsonObject(obj)))
      )
    )
  }

given jsonFacade: JsonFacade[Json] =
  new JsonFacade[Json] {
    def asString(j: Json): Option[String] = j.asString
    def asInt(j: Json): Option[Int] = j.asNumber.flatMap(_.toInt)
    def asMap(j: Json): Option[Map[String, Json]] = j.asObject.map(_.toMap)
    def asVector(j: Json): Option[Vector[Json]] = j.asArray
    def fromString(s: String): Json = Json.fromString(s)
    def fromInt(i: Int): Json = Json.fromInt(i)
    def fromFields(fs: (String, Json)*): Json = Json.fromFields(fs)
    def fromValues(vs: Json*): Json = Json.fromValues(vs)
    def nullValue: Json = Json.Null
  }

given jsonEncoder[A](using enc: Encoder[A]): JsonEncoder[Json, A] =
  JsonEncoder.instance[Json, A](enc.apply)

given jsonDecoder[A](using dec: Decoder[A]): JsonDecoder[Json, A] =
  JsonDecoder.instance[Json, A](j => dec.decodeJson(j).toOption)
