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

package jsonrpc4cats.http4s

import munit.CatsEffectSuite
import munit.Location

import scala.collection.mutable

import cats.Applicative
import cats.data.Kleisli
import cats.data.OptionT
import cats.effect.IO
import io.circe.Json
import org.http4s.HttpApp
import org.http4s.MediaType
import org.http4s.Method
import org.http4s.Request
import org.http4s.Status
import org.http4s.headers.`Content-Type`
import org.http4s.implicits.*
import org.http4s.server.AuthMiddleware
import org.http4s.server.Router

import jsonrpc4cats.*
import jsonrpc4cats.circe.given
import jsonrpc4cats.server.*

class RpcServiceTests extends CatsEffectSuite {

  /*
   * Functions for the common checks
   */

  def commonTests(prefix: String, service: HttpApp[IO])(using loc: Location): Unit = {

    test(s"${prefix}: A non root path") {

      val req = Request[IO](method = Method.GET, uri = uri"/rpc/abc")

      service.run(req).map { res =>
        assert(res.status == Status.NotFound)
      }

    }

    test(s"${prefix}: Not a POST request") {

      val req = Request[IO](method = Method.GET, uri = uri"/rpc")

      service.run(req).map { res =>
        assert(res.status == Status.MethodNotAllowed)
      }

    }

    test(s"${prefix}: Content-Type header is missing") {

      val req = Request[IO](method = Method.POST, uri = uri"/rpc")

      service.run(req).map { res =>
        assert(res.status == Status.UnsupportedMediaType)
      }

    }

    test(s"${prefix}: Content-Type header is not application/json") {

      val req = Request[IO](method = Method.POST, uri = uri"/rpc")
        .withContentType(`Content-Type`(MediaType.multipart.`form-data`))

      service.run(req).map { res =>
        assert(res.status == Status.UnsupportedMediaType)
      }

    }

    test(s"${prefix}: A valid Request") {

      val req = """{"jsonrpc":"2.0","method":"calc.sum","params":[1,2],"id":0}"""
      val exp = """{"jsonrpc":"2.0","result":3,"id":0}"""

      val rio = Request[IO](method = Method.POST, uri = uri"/rpc")
        .withEntity(req)
        .withContentType(`Content-Type`(MediaType.application.json))

      service.run(rio).flatMap { res =>
        for {
          s <- IO.pure(res.status)
          c <- IO.pure(res.headers.get[`Content-Type`].map(_.mediaType))
          b <- res.as[String]
        } yield {
          assertEquals(s, Status.Ok)
          assertEquals(c, Some(MediaType.application.json))
          assertEquals(b, exp)
        }
      }

    }

    test(s"${prefix}: A valid Notification") {

      val req = """{"jsonrpc":"2.0","method":"calc.sum","params":[1,2]}"""

      val rio = Request[IO](method = Method.POST, uri = uri"/rpc")
        .withEntity(req)
        .withContentType(`Content-Type`(MediaType.application.json))

      service.run(rio).flatMap { res =>
        for {
          s <- IO.pure(res.status)
          c <- IO.pure(res.headers.get[`Content-Type`].map(_.mediaType))
          b <- res.as[String]
        } yield {
          assertEquals(s, Status.Accepted)
          assertEquals(c, None)
          assertEquals(b, "")
        }
      }

    }

  }

  def onErrorTest(prefix: String, createService: mutable.ListBuffer[RpcErrorInfo] => HttpApp[IO])(using
    loc: Location
  ): Unit =
    test(s"${prefix}: onError test") {

      val log = mutable.ListBuffer.empty[RpcErrorInfo]

      val serviceWithLogger = createService(log)

      val req = """{"jsonrpc":"2.0","method":"nonexistent","params":[1,2],"id":0}"""

      val rio = Request[IO](method = Method.POST, uri = uri"/rpc")
        .withEntity(req)
        .withContentType(`Content-Type`(MediaType.application.json))

      serviceWithLogger.run(rio).map { _ =>
        assert(log.size > 0)
      }

    }

  /*
   * RpcService.httpRoutes tests
   */

  object Rpc {

    def calcSum[F[_]](using F: Applicative[F]) =
      RpcMethod.instance[F, "calc.sum", (Int, Int), RpcErr, Long] { (a, b) =>
        F.pure(Right(a.toLong + b.toLong))
      }

    def api[F[_]: Applicative] = RpcServer.add(calcSum[F])

    def service = Router("/rpc" -> RpcService.httpRoutes[Json](api[IO])).orNotFound

    def serviceWithLogger(log: mutable.ListBuffer[RpcErrorInfo]) =
      Router("/rpc" -> RpcService.httpRoutes[Json](api[IO], err => IO(log.append(err)))).orNotFound

  }

  commonTests("RpcService.httpRoutes", Rpc.service)

  onErrorTest("RpcService.httpRoutes", Rpc.serviceWithLogger)

  /*
   * RpcService.authedRoutes tests
   */

  final case class User(role: String)

  object RpcWithAuth {

    def calcSum[F[_]](using F: Applicative[F]) =
      RpcMethod.withAuthIf[F, User, "calc.sum", (Int, Int), RpcErr, Long](_.role == "admin") {
        case (_, (a, b)) =>
          F.pure(Right(a.toLong + b.toLong))
      }

    def api[F[_]: Applicative] = RpcServer.add(calcSum[F])

    def middleware(user: User): AuthMiddleware[IO, User] =
      AuthMiddleware[IO, User](Kleisli(_ => OptionT.fromOption[IO](Some(user))))

    def service(user: User) =
      Router("/rpc" -> middleware(user)(RpcService.authedRoutes[Json](api[IO]))).orNotFound

    def serviceWithLogger(user: User)(log: mutable.ListBuffer[RpcErrorInfo]) =
      Router("/rpc" -> middleware(user)(RpcService.authedRoutes[Json](api[IO], err => IO(log.append(err))))).orNotFound

  }

  commonTests("RpcService.authedRoutes", RpcWithAuth.service(User("admin")))

  onErrorTest("RpcService.authedRoutes", RpcWithAuth.serviceWithLogger(User("admin")))

  test("RpcService.authedRoutes: An unauthorized request") {

    val req = """{"jsonrpc":"2.0","method":"calc.sum","params":[1,2],"id":0}"""

    val rio = Request[IO](method = Method.POST, uri = uri"/rpc")
      .withEntity(req)
      .withContentType(`Content-Type`(MediaType.application.json))

    RpcWithAuth.service(User("user")).run(rio).map { res =>
      assert(res.status == Status.Unauthorized)
    }

  }

}
