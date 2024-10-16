package jsonrpc4cats.example

import munit.CatsEffectSuite

import cats.effect.IO
import org.http4s.MediaType
import org.http4s.Method
import org.http4s.Request
import org.http4s.Status
import org.http4s.headers.`Content-Type`
import org.http4s.implicits.*

class Http4sAppTests extends CatsEffectSuite {

  import Http4sApp.httpApp

  test("pub.sum") {
    val req = """{"jsonrpc":"2.0","method":"pub.sum","params":[1,2],"id":0}"""
    val exp = """{"jsonrpc":"2.0","result":3,"id":0}"""

    val rio = Request[IO](method = Method.POST, uri = uri"/rpc")
      .withEntity(req)
      .withContentType(`Content-Type`(MediaType.application.json))

    httpApp.run(rio).flatMap(_.as[String]).map { res =>
      assertEquals(res, exp)
    }
  }

  test("sec.sum") {
    val req = """{"jsonrpc":"2.0","method":"sec.sum","params":[1,2],"id":0}"""
    val exp = """{"jsonrpc":"2.0","result":"User123: 3","id":0}"""

    val rio = Request[IO](method = Method.POST, uri = uri"/secured/rpc")
      .withEntity(req)
      .withContentType(`Content-Type`(MediaType.application.json))

    httpApp.run(rio).flatMap(_.as[String]).map { res =>
      assertEquals(res, exp)
    }
  }

  test("sec.mul") {
    val req = """{"jsonrpc":"2.0","method":"sec.mul","params":[1,2],"id":0}"""

    val rio = Request[IO](method = Method.POST, uri = uri"/secured/rpc")
      .withEntity(req)
      .withContentType(`Content-Type`(MediaType.application.json))

    httpApp.run(rio).map { res =>
      assertEquals(res.status, Status.Unauthorized)
    }
  }

}
