package jsonrpc4cats.example

import munit.FunSuite

import io.circe.Json

import jsonrpc4cats.circe.given

class HelloCalcTests extends FunSuite {

  type Eff[A] = Either[Throwable, A]

  test("hello.name") {
    val req = """{"jsonrpc":"2.0","method":"hello.name","params":["Jack"],"id":1}"""
    val exp = """{"jsonrpc":"2.0","result":"Hello, Jack!","id":1}"""

    val res = HelloCalc.api[Eff].handle[Json](req)

    assertEquals(res.map(_.noSpaces).value, Right(Some(exp)))
  }

  test("calc.add") {
    val req = """{"jsonrpc":"2.0","method":"calc.add","params":[3, 5],"id":1}"""
    val exp = """{"jsonrpc":"2.0","result":8,"id":1}"""

    val res = HelloCalc.api[Eff].handle[Json](req)

    assertEquals(res.map(_.noSpaces).value, Right(Some(exp)))
  }

}
