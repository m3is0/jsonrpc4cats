# Creating a Server

## Dependencies

Add the following dependencies to your `build.sbt`:
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
    ToRpcError.instance {
      case DivByZero =>
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

## Method Parameters

Method parameters must be of a product type:
```scala
package jsonrpc4cats.example

import cats.Applicative

import jsonrpc4cats.RpcErr
import jsonrpc4cats.server.*

object HelloServer {

  // a method without parameters
  def hello[F[_]](using F: Applicative[F]) =
    RpcMethod.instance[F, "hello.hello", EmptyTuple, RpcErr, String] { _ =>
      F.pure(Right("Hello!"))
    }

  // a method with a single non-product parameter
  def helloName[F[_]](using F: Applicative[F]) =
    RpcMethod.instance[F, "hello.name", Tuple1[String], RpcErr, String] { params =>
      F.pure(Right(s"Hello, ${params.head}!"))
    }

  // a method with a single non-product parameter, using the RpcMethod1 API
  def helloName1[F[_]](using F: Applicative[F]) =
    RpcMethod1.instance[F, "hello.name1", String, RpcErr, String] { name =>
      F.pure(Right(s"Hello, ${name}!"))
    }

  def api[F[_]: Applicative] =
    RpcServer
      .add(hello[F])
      .add(helloName[F])
      .add(helloName1[F])
}

```

## Combining Servers

Servers can be combined as shown below:
```scala
package jsonrpc4cats.example

import cats.Applicative

object HelloCalc {

  // combining servers using the :+: operator, which is an alias for the 'extend' method
  def api[F[_]: Applicative] =
    HelloServer.api[F] :+: CalcServer.api[F]
}

```

## Custom Types

To use custom types, provide codecs for the JSON library you are using:
```scala
package jsonrpc4cats.example

import cats.Applicative
import cats.MonadError
import cats.data.OptionT
import io.circe.Decoder
import io.circe.Encoder
import io.circe.Json
import io.circe.generic.semiauto.*
import io.circe.syntax.*

import jsonrpc4cats.*
import jsonrpc4cats.circe.given
import jsonrpc4cats.server.*

object CustomTypes {

  sealed trait DivError
  final case class DivByZero(divident: Int) extends DivError

  given toRpcError: ToRpcError[Json, DivError] =
    ToRpcError.instance {
      case DivByZero(a) =>
        RpcError(RpcErrorCode(1000), "Division by zero", Some(Map("divident" -> a).asJson))
    }

  final case class DivParams(divident: Int, divisor: Int)
  given paramsDecoder: Decoder[DivParams] = deriveDecoder[DivParams]

  final case class DivResult(quotient: Int, remainder: Int)
  given resultEncoder: Encoder[DivResult] = deriveEncoder[DivResult]

  def div[F[_]](using F: Applicative[F]) =
    RpcMethod.instance[F, "custom.div", DivParams, DivError, DivResult] {
      case DivParams(a, 0) =>
        F.pure(Left(DivByZero(a)))
      case DivParams(a, b) =>
        F.pure(Right(DivResult(a / b, a % b)))
    }

  def handle[F[_]](req: String)(using MonadError[F, Throwable]): OptionT[F, Json] =
    RpcServer.add(div[F]).handle[Json](req)
}

```
