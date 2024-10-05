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

class RpcMethodTests extends FunSuite {

  final case class User(role: String)

  test("RpcMethod.instance") {
    val m = RpcMethod.instance[Id, "abc", (Int, Int), Unit, Int] { (a, b) =>
      Right(a + b)
    }

    assertEquals(m((1, 2)), Right(3))
  }

  test("RpcMethod.withAuth") {
    val m = RpcMethod.withAuth[Id, User, "abc", (Int, Int), Unit, String] { case (user, (a, b)) =>
      Right(s"${user.role}_${a}_${b}")
    }

    assertEquals(m((1, 2)).run(User("admin")).value, Some(Right("admin_1_2")))
  }

  test("RpcMethod.withAuthIf") {
    val m = RpcMethod.withAuthIf[Id, User, "abc", (Int, Int), Unit, String](_.role == "admin") { case (user, (a, b)) =>
      Right(s"${user.role}_${a}_${b}")
    }

    assertEquals(m((1, 2)).run(User("admin")).value, Some(Right("admin_1_2")))
    assertEquals(m((1, 2)).run(User("user")).value, None)
  }

}
