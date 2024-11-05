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

package jsonrpc4cats.server

import munit.FunSuite

import cats.Id

class RpcServerTests extends FunSuite {

  private def noop[K <: String & Singleton] =
    RpcMethod.instance[Id, K, EmptyTuple, Unit, Unit](_ => Right(()))

  test("RpcServer.add should not compile with non distinct methods") {

    val code = """
      RpcServer
        .add(noop["m1"])
        .add(noop["m2"])
        .add(noop["m2"])
    """
    assert(compileErrors(code).contains("DistinctMethods"))
  }

  test("RpcServer.add test") {

    val srv =
      RpcServer
        .add(noop["m1"])
        .add(noop["m2"])

    assert(srv("m1").isDefined)
    assert(srv("m2").isDefined)
  }

  test("RpcServer.extend test") {

    val srv1 = RpcServer.add(noop["m1"])
    val srv2 = RpcServer.add(noop["m2"])

    val srv = srv1.extend(srv2)

    assert(srv("m1").isDefined)
    assert(srv("m2").isDefined)
  }

  test("RpcServer.:+: test") {

    val srv1 = RpcServer.add(noop["m1"])
    val srv2 = RpcServer.add(noop["m2"])

    val srv = srv1 :+: srv2

    assert(srv("m1").isDefined)
    assert(srv("m2").isDefined)
  }

  test("RpcServer.apply test") {

    val srv = RpcServer.add(noop["m1"])

    assert(srv("m1").isDefined)
    assert(srv("m2").isEmpty)
  }

}
