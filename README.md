# jsonrpc4cats
A typesafe implementation of [JSON-RPC 2.0](https://www.jsonrpc.org/specification) protocol (server only).

## Quick example
```scala
package jsonrpc4cats.example

import cats.Applicative
import cats.MonadError
import io.circe.Json

import jsonrpc4cats.circe.given
import jsonrpc4cats.server.*

object Calc {

  // Define errors

  sealed trait Err
  case object DivByZero extends Err

  given errToRpcError[J]: ToRpcError[J, Err] =
    ToRpcError.instance { case DivByZero =>
      RpcError(RpcErrorCode(1000), "Division by zero")
    }

  // Define methods

  def add[F[_]](using F: Applicative[F]) =
    RpcMethod.instance[F, "calc.add", (Int, Int), Err, Long] { (a, b) =>
      F.pure(Right(a.toLong + b.toLong))
    }

  def subtract[F[_]](using F: Applicative[F]) =
    RpcMethod.instance[F, "calc.sub", (Int, Int), Err, Long] { (a, b) =>
      F.pure(Right(a.toLong - b.toLong))
    }

  def multiply[F[_]](using F: Applicative[F]) =
    RpcMethod.instance[F, "calc.mul", (Int, Int), Err, Long] { (a, b) =>
      F.pure(Right(a.toLong * b.toLong))
    }

  def divide[F[_]](using F: Applicative[F]) =
    RpcMethod.instance[F, "calc.div", (Int, Int), Err, (Int, Int)] {
      case (_, 0) =>
        F.pure(Left(DivByZero))
      case (a, b) =>
        F.pure(Right((a / b, a % b)))
    }

  // Define API (uniqueness of method names is checked at compile time)

  def api[F[_]: Applicative] =
    RpcServer
      .add(add[F])
      .add(subtract[F])
      .add(multiply[F])
      .add(divide[F])

  // Handle a request (can be used any JSON library for which a module exists)

  def handle[F[_]](req: String)(using MonadError[F, Throwable]): F[Option[Json]] =
    api[F].handle[Json](req)
}

```
