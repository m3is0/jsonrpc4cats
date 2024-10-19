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
