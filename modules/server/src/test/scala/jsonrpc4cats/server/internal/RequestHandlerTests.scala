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

package jsonrpc4cats.server.internal

import munit.FunSuite

import cats.Applicative
import cats.ApplicativeError
import cats.syntax.all.*
import io.circe.Decoder
import io.circe.HCursor
import io.circe.Json
import io.circe.syntax.*

import jsonrpc4cats.RpcErr
import jsonrpc4cats.RpcError
import jsonrpc4cats.RpcErrorCode
import jsonrpc4cats.ToRpcError
import jsonrpc4cats.circe.given
import jsonrpc4cats.server.*

class RequestHandlerTests extends FunSuite {

  type Eff[A] = Either[Throwable, A]

  private def noop[F[_]](using F: Applicative[F]) =
    RpcMethod.instance[F, "noop", EmptyTuple, RpcErr, Unit] { _ =>
      F.pure(Right(()))
    }

  /*
   * Specification tests - https://www.jsonrpc.org/specification
   *
   */

  test("Rpc call with positional parameters") {

    def subtract[F[_]](using F: Applicative[F]) =
      RpcMethod.instance[F, "subtract", (Int, Int), RpcErr, Int] { (a, b) =>
        F.pure(Right(a - b))
      }

    def srv[F[_]: Applicative] =
      RpcServer.add(subtract[F])

    val req1 = """{"jsonrpc":"2.0","method":"subtract","params":[42, 23],"id":1}"""
    val exp1 = """{"jsonrpc":"2.0","result":19,"id":1}"""
    val res1 = srv[Eff].handle[Json](req1)

    val req2 = """{"jsonrpc":"2.0","method":"subtract","params":[23, 42],"id":2}"""
    val exp2 = """{"jsonrpc":"2.0","result":-19,"id":2}"""
    val res2 = srv[Eff].handle[Json](req2)

    assertEquals(res1.map(_.noSpaces).value, Right(Some(exp1)))
    assertEquals(res2.map(_.noSpaces).value, Right(Some(exp2)))

  }

  test("Rpc call with named parameters") {

    final case class SubtractParams(minuend: Int, subtrahend: Int)

    object SubtractParams {
      given jsonDecoder: Decoder[SubtractParams] with {
        final def apply(c: HCursor): Decoder.Result[SubtractParams] =
          for {
            m <- c.downField("minuend").as[Int]
            s <- c.downField("subtrahend").as[Int]
          } yield SubtractParams(m, s)
      }
    }

    def subtract[F[_]](using F: Applicative[F]) =
      RpcMethod.instance[F, "subtract", SubtractParams, RpcErr, Int] { par =>
        F.pure(Right(par.minuend - par.subtrahend))
      }

    def srv[F[_]: Applicative] =
      RpcServer.add(subtract[F])

    val req1 = """{"jsonrpc":"2.0","method":"subtract","params":{"subtrahend":23,"minuend":42},"id":3}"""
    val exp1 = """{"jsonrpc":"2.0","result":19,"id":3}"""
    val res1 = srv[Eff].handle[Json](req1)

    val req2 = """{"jsonrpc":"2.0","method":"subtract","params":{"minuend":42,"subtrahend":23},"id":4}"""
    val exp2 = """{"jsonrpc":"2.0","result":19,"id":4}"""
    val res2 = srv[Eff].handle[Json](req2)

    assertEquals(res1.map(_.noSpaces).value, Right(Some(exp1)))
    assertEquals(res2.map(_.noSpaces).value, Right(Some(exp2)))

  }

  test("A Notification") {

    def update[F[_]](using F: Applicative[F]) =
      RpcMethod.instance[F, "update", (Int, Int, Int, Int, Int), RpcErr, Unit] { _ =>
        F.pure(Right(()))
      }

    def foobar[F[_]](using F: Applicative[F]) =
      RpcMethod.instance[F, "foobar", EmptyTuple, RpcErr, Unit] { _ =>
        F.pure(Right(()))
      }

    def srv[F[_]: Applicative] =
      RpcServer.add(update[F]).add(foobar[F])

    val req1 = """{"jsonrpc":"2.0","method":"update","params":[1,2,3,4,5]}"""
    val res1 = srv[Eff].handle[Json](req1)

    val req2 = """{"jsonrpc":"2.0","method":"foobar"}"""
    val res2 = srv[Eff].handle[Json](req2)

    assert(res1.value == Right(None))
    assert(res2.value == Right(None))

  }

  test("Rpc call of non-existent method") {

    def srv[F[_]: Applicative] =
      RpcServer.add(noop[F])

    val req = """{"jsonrpc":"2.0","method":"foobar","id":"1"}"""
    val exp = """{"jsonrpc":"2.0","error":{"code":-32601,"message":"Method not found"},"id":"1"}"""
    val res = srv[Eff].handle[Json](req)

    assertEquals(res.map(_.noSpaces).value, Right(Some(exp)))

  }

  test("Rpc call with invalid JSON") {

    def srv[F[_]: Applicative] =
      RpcServer.add(noop[F])

    val req = """{"jsonrpc":"2.0","method":"foobar,"params":"bar","baz]"""
    val exp = """{"jsonrpc":"2.0","error":{"code":-32700,"message":"Parse error"},"id":null}"""
    val res = srv[Eff].handle[Json](req)

    assertEquals(res.map(_.noSpaces).value, Right(Some(exp)))

  }

  test("Rpc call with invalid Request object") {

    def srv[F[_]: Applicative] =
      RpcServer.add(noop[F])

    val req = """{"jsonrpc":"2.0","method":1,"params":"bar"}"""
    val exp = """{"jsonrpc":"2.0","error":{"code":-32600,"message":"Invalid Request"},"id":null}"""
    val res = srv[Eff].handle[Json](req)

    assertEquals(res.map(_.noSpaces).value, Right(Some(exp)))

  }

  test("Rpc call Batch, invalid JSON") {

    def srv[F[_]: Applicative] =
      RpcServer.add(noop[F])

    val r1 = """{"jsonrpc": "2.0", "method": "sum", "params": [1,2,4], "id": "1"}"""
    val r2 = """{"jsonrpc": "2.0", "method""""
    val req = s"[$r1,$r2]"
    val exp = """{"jsonrpc":"2.0","error":{"code":-32700,"message":"Parse error"},"id":null}"""
    val res = srv[Eff].handle[Json](req)

    assertEquals(res.map(_.noSpaces).value, Right(Some(exp)))

  }

  test("Rpc call with an empty Array") {

    def srv[F[_]: Applicative] =
      RpcServer.add(noop[F])

    val req = "[]"
    val exp = """{"jsonrpc":"2.0","error":{"code":-32600,"message":"Invalid Request"},"id":null}"""
    val res = srv[Eff].handle[Json](req)

    assertEquals(res.map(_.noSpaces).value, Right(Some(exp)))

  }

  test("Rpc call with an invalid Batch (but not empty)") {

    def srv[F[_]: Applicative] =
      RpcServer.add(noop[F])

    val req = "[1]"
    val exp = """[{"jsonrpc":"2.0","error":{"code":-32600,"message":"Invalid Request"},"id":null}]"""
    val res = srv[Eff].handle[Json](req)

    assertEquals(res.map(_.noSpaces).value, Right(Some(exp)))

  }

  test("Rpc call with invalid Batch") {

    def srv[F[_]: Applicative] =
      RpcServer.add(noop[F])

    val req = "[1,2,3]"
    val e = """{"jsonrpc":"2.0","error":{"code":-32600,"message":"Invalid Request"},"id":null}"""
    val exp = s"[$e,$e,$e]"
    val res = srv[Eff].handle[Json](req)

    assertEquals(res.map(_.noSpaces).value, Right(Some(exp)))

  }

  test("Rpc call Batch") {

    def sum[F[_]](using F: Applicative[F]) =
      RpcMethod.instance[F, "sum", (Int, Int, Int), RpcErr, Int] { (a, b, c) =>
        F.pure(Right(a + b + c))
      }

    def notifyHello[F[_]](using F: Applicative[F]) =
      RpcMethod.instance[F, "notify_hello", Tuple1[Int], RpcErr, Unit] { _ =>
        F.pure(Right(()))
      }

    def subtract[F[_]](using F: Applicative[F]) =
      RpcMethod.instance[F, "subtract", (Int, Int), RpcErr, Int] { (a, b) =>
        F.pure(Right(a - b))
      }

    def getData[F[_]](using F: Applicative[F]) =
      RpcMethod.instance[F, "get_data", EmptyTuple, RpcErr, (String, Int)] { _ =>
        F.pure(Right(("hello", 5)))
      }

    def srv[F[_]: Applicative] =
      RpcServer
        .add(sum[F])
        .add(notifyHello[F])
        .add(subtract[F])
        .add(getData[F])

    val r1 = """{"jsonrpc":"2.0","method":"sum","params":[1,2,4],"id":"1"}"""
    val r2 = """{"jsonrpc":"2.0","method":"notify_hello","params":[7]}"""
    val r3 = """{"jsonrpc":"2.0","method":"subtract","params":[42,23],"id":"2"}"""
    val r4 = """{"foo":"boo"}"""
    val r5 = """{"jsonrpc":"2.0","method":"foo.get","params":{"name":"myself"},"id":"5"}"""
    val r6 = """{"jsonrpc":"2.0","method":"get_data","id":"9"}"""
    val req = s"[$r1,$r2,$r3,$r4,$r5,$r6]"

    val e1 = """{"jsonrpc":"2.0","result":7,"id":"1"}"""
    val e2 = """{"jsonrpc":"2.0","result":19,"id":"2"}"""
    val e3 = """{"jsonrpc":"2.0","error":{"code":-32600,"message":"Invalid Request"},"id":null}"""
    val e4 = """{"jsonrpc":"2.0","error":{"code":-32601,"message":"Method not found"},"id":"5"}"""
    val e5 = """{"jsonrpc":"2.0","result":["hello",5],"id":"9"}"""
    val exp = s"[$e1,$e2,$e3,$e4,$e5]"

    val res = srv[Eff].handle[Json](req)

    assertEquals(res.map(_.noSpaces).value, Right(Some(exp)))

  }

  test("Rpc call Batch (all notifications)") {

    def notifySum[F[_]](using F: Applicative[F]) =
      RpcMethod.instance[F, "notify_sum", (Int, Int, Int), RpcErr, Unit] { _ =>
        F.pure(Right(()))
      }

    def notifyHello[F[_]](using F: Applicative[F]) =
      RpcMethod.instance[F, "notify_hello", Tuple1[Int], RpcErr, Unit] { _ =>
        F.pure(Right(()))
      }

    def srv[F[_]: Applicative] =
      RpcServer.add(notifySum[F]).add(notifyHello[F])

    val r1 = """{"jsonrpc":"2.0","method":"notify_sum","params":[1,2,4]}"""
    val r2 = """{"jsonrpc":"2.0","method":"notify_hello","params":[7]}"""
    val req = s"[$r1,$r2]"
    val res = srv[Eff].handle[Json](req)

    assert(res.value == Right(None))

  }

  /*
   * Non-specification tests
   *
   */

  private def logError[F[_]](log: collection.mutable.ListBuffer[RpcErrorInfo[Json]])(using
    F: ApplicativeError[F, Throwable]
  ): RpcErrorInfo[Json] => F[Unit] =
    err => F.catchNonFatal(log.append(err)) *> F.pure(())

  test("A custom error type") {

    final case class CalcError(message: String, a: Int, b: Int)

    object CalcError {
      given toRpcError: ToRpcError[Json, CalcError] =
        ToRpcError.instance { err =>
          RpcError(
            RpcErrorCode(1000),
            err.message,
            Some(Map("a" -> err.a, "b" -> err.b).asJson)
          )
        }
    }

    def divide[F[_]](using F: Applicative[F]) =
      RpcMethod.instance[F, "divide", (Int, Int), CalcError, String] {
        case (a, 0) =>
          F.pure(Left(CalcError("Division by zero", a, 0)))
        case (a, b) =>
          F.pure(Right((a / b).toString))
      }

    def srv[F[_]: Applicative] =
      RpcServer.add(divide[F])

    val log = collection.mutable.ListBuffer.empty[RpcErrorInfo[Json]]

    val req = """{"jsonrpc":"2.0","method":"divide","params":[21, 0],"id":1}"""
    val res = srv[Eff].handle[Json](req, logError(log))

    val expRes = """{"jsonrpc":"2.0","error":{"code":1000,"message":"Division by zero","data":{"a":21,"b":0}},"id":1}"""
    val expLog = List(RpcErrorInfo(1000, "Division by zero", Some(Map("a" -> 21, "b" -> 0).asJson), req, None))

    assertEquals(res.map(_.noSpaces).value, Right(Some(expRes)))
    assertEquals(log.toList, expLog)

  }

  test("An exception inside a method body") {

    val ex = new Exception("error")

    def foobar[F[_]](using F: ApplicativeError[F, Throwable]) =
      RpcMethod.instance[F, "foobar", (Int, Int), RpcErr, Unit] { _ =>
        F.catchNonFatal(throw ex) *> F.pure(Right(()))
      }

    def srv[F[_]](using ApplicativeError[F, Throwable]) =
      RpcServer.add(foobar[F])

    val log = collection.mutable.ListBuffer.empty[RpcErrorInfo[Json]]

    val req = """{"jsonrpc":"2.0","method":"foobar","params":[21, 0],"id":1}"""
    val res = srv[Eff].handle[Json](req, logError(log))

    val expRes = """{"jsonrpc":"2.0","error":{"code":-32603,"message":"Internal error"},"id":1}"""
    val expLog = List(RpcErrorInfo[Json](-32603, "Internal error", None, req, Some(ex)))

    assertEquals(res.map(_.noSpaces).value, Right(Some(expRes)))
    assertEquals(log.toList, expLog)

  }

  test("A server error") {

    def srv[F[_]: Applicative] =
      RpcServer.add(noop[F])

    val log = collection.mutable.ListBuffer.empty[RpcErrorInfo[Json]]

    val req = """{"jsonrpc":"2.0","method":"divide","params":[21, 0],"id":1}"""
    val res = srv[Eff].handle[Json](req, logError(log))

    val expRes = """{"jsonrpc":"2.0","error":{"code":-32601,"message":"Method not found"},"id":1}"""
    val expLog = List(RpcErrorInfo[Json](-32601, "Method not found", None, req, None))

    assertEquals(res.map(_.noSpaces).value, Right(Some(expRes)))
    assertEquals(log.toList, expLog)

  }

}
