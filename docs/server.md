# Creating a Server

## Dependencies

Add the following dependencies to your ```build.sbt```:
```scala
libraryDependencies ++= Seq(
  "io.github.m3is0" %% "jsonrpc4cats-circe" % "@VERSION@",
  "io.github.m3is0" %% "jsonrpc4cats-server" % "@VERSION@"
)
```

## A Basic Example

An annotated step-by-step example:
```scala
package jsonrpc4cats.example

import cats.Applicative
import cats.MonadError
import cats.data.OptionT
import io.circe.Json

import jsonrpc4cats.*
import jsonrpc4cats.circe.given
import jsonrpc4cats.server.*

object CalcServer {

  // 1. Define errors

  sealed trait Err
  case object DivByZero extends Err

  given errToRpcError[J]: ToRpcError[J, Err] =
    ToRpcError.instance { case DivByZero =>
      RpcError(RpcErrorCode(1000), "Division by zero")
    }

  // 2. Define methods

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

  // 3. Define API (uniqueness of method names is checked at compile time)

  def api[F[_]: Applicative] =
    RpcServer
      .add(add[F])
      .add(subtract[F])
      .add(multiply[F])
      .add(divide[F])

  // 4. Handle a request (you can use any JSON library for which a module exists)

  def handle[F[_]](req: String)(using MonadError[F, Throwable]): OptionT[F, Json] =
    api[F].handle[Json](req)
}

```
