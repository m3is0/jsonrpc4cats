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
