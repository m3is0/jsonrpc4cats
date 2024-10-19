package jsonrpc4cats.example

import munit.FunSuite

import io.circe.Json

import jsonrpc4cats.circe.given

class HelloServerTests extends FunSuite {

  type Eff[A] = Either[Throwable, A]

  test("hello.hello") {
    val req = """{"jsonrpc":"2.0","method":"hello.hello","id":1}"""
    val exp = """{"jsonrpc":"2.0","result":"Hello!","id":1}"""

    val res = HelloServer.api[Eff].handle[Json](req)

    assertEquals(res.map(_.noSpaces).value, Right(Some(exp)))
  }

  test("hello.name") {
    val req = """{"jsonrpc":"2.0","method":"hello.name","params":["Jack"],"id":1}"""
    val exp = """{"jsonrpc":"2.0","result":"Hello, Jack!","id":1}"""

    val res = HelloServer.api[Eff].handle[Json](req)

    assertEquals(res.map(_.noSpaces).value, Right(Some(exp)))
  }

}
