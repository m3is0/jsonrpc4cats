package jsonrpc4cats.example

import cats.Applicative

import jsonrpc4cats.RpcErr
import jsonrpc4cats.server.*

object HelloServer {

  // a method without params
  def hello[F[_]](using F: Applicative[F]) =
    RpcMethod.instance[F, "hello.hello", EmptyTuple, RpcErr, String] { _ =>
      F.pure(Right("Hello!"))
    }

  // a method with a single, non-product param
  def helloName[F[_]](using F: Applicative[F]) =
    RpcMethod.instance[F, "hello.name", Tuple1[String], RpcErr, String] { params =>
      F.pure(Right(s"Hello, ${params.head}!"))
    }

  def api[F[_]: Applicative] =
    RpcServer
      .add(hello[F])
      .add(helloName[F])
}
