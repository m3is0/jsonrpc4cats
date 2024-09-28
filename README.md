# jsonrpc4cats

![CI](https://github.com/m3is0/jsonrpc4cats/actions/workflows/ci.yml/badge.svg)

A simple and type-safe implementation of [JSON-RPC 2.0](https://www.jsonrpc.org/specification) protocol (server and client). 
Available for Scala 3.3 and above, cross-built for JVM, JS and Native platforms.

- [Creating a Server](#creating-a-server)
- [Creating a Client](#creating-a-client)

## Creating a Server

Add the following dependencies to your ```build.sbt```:
```scala
libraryDependencies ++= Seq(
  "io.github.m3is0" %% "jsonrpc4cats-circe" % "<version>",
  "io.github.m3is0" %% "jsonrpc4cats-server" % "<version>"
)
```

### A Basic Example

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

## Creating a Client

Add the following dependencies to your ```build.sbt```:
```scala
libraryDependencies ++= Seq(
  "io.github.m3is0" %% "jsonrpc4cats-circe" % "<version>",
  "io.github.m3is0" %% "jsonrpc4cats-client" % "<version>"
)
```

### A Basic Example

```scala
package jsonrpc4cats.example

import cats.Applicative
import cats.MonadError
import cats.data.EitherT
import cats.syntax.all.*
import io.circe.Json

import jsonrpc4cats.*
import jsonrpc4cats.circe.given
import jsonrpc4cats.client.*

object CalcClient {

  import RpcCall.*

  // 1. Define errors (it's also possible to define errors in a shared module)

  sealed trait Err
  case object DivByZero extends Err

  given errFromRpcError[J]: FromRpcError[J, Err] =
    FromRpcError.instance {
      case RpcError(RpcErrorCode(1000), _, _) => Some(DivByZero)
      case _ => None
    }

  // 2. Define a function used to send a request and receive a response

  def send[F[_]](req: Json)(using MonadError[F, Throwable]): F[Option[String]] =
    CalcServer.handle[F](req.noSpaces).map(_.noSpaces).value

  // 3. Define helpers for building requests (optional)

  def add(a: Int, b: Int) =
    RpcRequest[(Int, Int), Int]("calc.add", (a, b))

  def mul(a: Int, b: Int) =
    RpcRequest[(Int, Int), Int]("calc.mul", (a, b))

  def div(a: Int, b: Int) =
    RpcRequest[(Int, Int), (Int, Int)]("calc.div", (a, b))

  // 4. Make some calls

  // an example function with an effect
  def printToConsole[F[_]: Applicative, A](a: A): F[Unit] =
    Applicative[F].pure(())

  def run[F[_]](using MonadError[F, Throwable]): EitherT[F, RpcCallError[Err], (Int, Int)] =
    call((add(2, 3), mul(5, 8)))
      .flatMap { (a, b) =>
        call(div(b, a)).flatMap { a =>
          liftF(printToConsole(a)) *> pure(a)
        }
      }
      .runWith[Err](send)
}

```
