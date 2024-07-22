# jsonrpc4cats

![CI](https://github.com/m3is0/jsonrpc4cats/actions/workflows/ci.yml/badge.svg)

A simple and type-safe implementation of [JSON-RPC 2.0](https://www.jsonrpc.org/specification) protocol (server only).

## Getting started
Add the following dependencies to your ```build.sbt```:
```scala
libraryDependencies ++= Seq(
  "io.github.m3is0" %% "jsonrpc4cats-circe" % "<version>",
  "io.github.m3is0" %% "jsonrpc4cats-server" % "<version>"
)
```
Available for Scala 3.3 and above.

## Basic example
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

  // Handle a request (you can use any JSON library for which a module exists)

  def handle[F[_]](req: String)(using MonadError[F, Throwable]): F[Option[Json]] =
    api[F].handle[Json](req)
}

```
