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

class CustomTypesTests extends FunSuite {

  type Eff[A] = Either[Throwable, A]

  test("custom.div") {
    val req = """{"jsonrpc":"2.0","method":"custom.div","params":{"divident":8,"divisor":3},"id":1}"""
    val exp = """{"jsonrpc":"2.0","result":{"quotient":2,"remainder":2},"id":1}"""

    val res = CustomTypes.handle[Eff](req)

    assertEquals(res.map(_.noSpaces).value, Right(Some(exp)))
  }

  test("custom.div with DivByZero") {
    val req = """{"jsonrpc":"2.0","method":"custom.div","params":{"divident":8,"divisor":0},"id":1}"""
    val exp = """{"jsonrpc":"2.0","error":{"code":1000,"message":"Division by zero","data":{"divident":8}},"id":1}"""

    val res = CustomTypes.handle[Eff](req)

    assertEquals(res.map(_.noSpaces).value, Right(Some(exp)))
  }

}
