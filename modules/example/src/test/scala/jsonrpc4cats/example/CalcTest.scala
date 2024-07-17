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

package jsonrpc4cats.example

import munit.FunSuite

class CalcTests extends FunSuite {

  type Eff[A] = Either[Throwable, A]

  test("calc.add") {
    val req = """{"jsonrpc":"2.0","method":"calc.add","params":[3, 5],"id":1}"""
    val exp = """{"jsonrpc":"2.0","result":8,"id":1}"""

    val res = Calc.handle[Eff](req)

    assertEquals(res.map(_.map(_.noSpaces)), Right(Some(exp)))
  }

  test("calc.sub") {
    val req = """{"jsonrpc":"2.0","method":"calc.sub","params":[5, 3],"id":1}"""
    val exp = """{"jsonrpc":"2.0","result":2,"id":1}"""

    val res = Calc.handle[Eff](req)

    assertEquals(res.map(_.map(_.noSpaces)), Right(Some(exp)))
  }

  test("calc.mul") {
    val req = """{"jsonrpc":"2.0","method":"calc.mul","params":[3, 5],"id":1}"""
    val exp = """{"jsonrpc":"2.0","result":15,"id":1}"""

    val res = Calc.handle[Eff](req)

    assertEquals(res.map(_.map(_.noSpaces)), Right(Some(exp)))
  }

  test("calc.div") {
    val req = """{"jsonrpc":"2.0","method":"calc.div","params":[5, 3],"id":1}"""
    val exp = """{"jsonrpc":"2.0","result":[1,2],"id":1}"""

    val res = Calc.handle[Eff](req)

    assertEquals(res.map(_.map(_.noSpaces)), Right(Some(exp)))
  }

  test("calc.div with DivByZero error") {
    val req = """{"jsonrpc":"2.0","method":"calc.div","params":[3, 0],"id":1}"""
    val exp = """{"jsonrpc":"2.0","error":{"code":1000,"message":"Division by zero"},"id":1}"""

    val res = Calc.handle[Eff](req)

    assertEquals(res.map(_.map(_.noSpaces)), Right(Some(exp)))
  }

}
