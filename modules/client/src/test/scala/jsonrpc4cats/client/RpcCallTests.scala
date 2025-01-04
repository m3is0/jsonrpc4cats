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

import munit.FunSuite

import io.circe.Json
import io.circe.parser.*

import jsonrpc4cats.*
import jsonrpc4cats.circe.given

class RpcCallTests extends FunSuite {

  import RpcCall.*

  type Eff[A] = Either[Throwable, A]

  def isEqualJson(js: Json, str: String): Boolean =
    parse(str).map(_ == js) match {
      case Right(true) => true
      case _ => false
    }

  test("A call with a single request") {

    val sreq = """{"jsonrpc":"2.0","method":"calc.add","params":[3, 5],"id":1}"""
    val sres = """{"jsonrpc":"2.0","result":8,"id":1}"""

    def send(jreq: Json): Eff[Option[String]] =
      if isEqualJson(jreq, sreq) then Right(Some(sres))
      else Left(new RuntimeException())

    val req = RpcRequest[(Int, Int), Int]("calc.add", (3, 5))

    val res = call[Eff](req).runWith[RpcErr](send).value

    assertEquals(res, Right(Right(8)))

  }

  test("A call with a single notification") {

    val sreq = """{"jsonrpc":"2.0","method":"calc.add","params":[3, 5]}"""

    def send(jreq: Json): Eff[Option[String]] =
      if isEqualJson(jreq, sreq) then Right(None)
      else Left(new RuntimeException())

    val req = RpcNotification[(Int, Int)]("calc.add", (3, 5))

    val res = call[Eff](req).runWith[RpcErr](send).value

    assertEquals(res, Right(Right(())))

  }

  test("A call with a batch request") {

    val sreq1 = """{"jsonrpc":"2.0","method":"calc.add","params":[2, 3],"id":1}"""
    val sreq2 = """{"jsonrpc":"2.0","method":"calc.add","params":[2, 3]}"""
    val sreq3 = """{"jsonrpc":"2.0","method":"calc.add","params":[3, 5],"id":3}"""
    val sreq = s"[${sreq1},${sreq2},${sreq3}]"

    val sres1 = """{"jsonrpc":"2.0","result":5,"id":1}"""
    val sres2 = """{"jsonrpc":"2.0","result":8,"id":3}"""
    val sres = s"[${sres1},${sres2}]"

    def send(jreq: Json): Eff[Option[String]] =
      if isEqualJson(jreq, sreq) then Right(Some(sres))
      else Left(new RuntimeException())

    val req1 = RpcRequest[(Int, Int), Int]("calc.add", (2, 3))
    val req2 = RpcNotification[(Int, Int)]("calc.add", (2, 3))
    val req3 = RpcRequest[(Int, Int), Int]("calc.add", (3, 5))

    val res = call[Eff]((req1, req2, req3)).runWith[RpcErr](send).value

    assertEquals(res, Right(Right((5, 8))))

  }

}
