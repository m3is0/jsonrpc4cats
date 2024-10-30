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

class RpcMethod1Tests extends FunSuite {

  final case class User(name: String, isAdmin: Boolean)

  test("RpcMethod1.instance") {

    val m = RpcMethod1.instance[Id, "abc", Int, Unit, Int] { a =>
      Right(a + 2)
    }

    assertEquals(m(Tuple1(1)), Right(3))
  }

  test("RpcMethod1.instance without type parameters") {

    val m: RpcMethod1[Id, "abc", Int, Unit, Int] =
      RpcMethod1.instance { a =>
        Right(a + 2)
      }

    assertEquals(m(Tuple1(1)), Right(3))
  }

  test("RpcMethod1.withAuth") {

    val m = RpcMethod1.withAuth[Id, User, "abc", Int, Unit, String] {
      case (user, a) =>
        Right(s"${user.name}_${a}")
    }

    assertEquals(m(Tuple1(1)).run(User("user", false)).value, Some(Right("user_1")))
  }

  test("RpcMethod1.withAuth without type parameters") {

    type AuthF = [x] =>> Auth[Id, User, x]

    val m: RpcMethod1[AuthF, "abc", Int, Unit, String] =
      RpcMethod1.withAuth {
        case (user, a) =>
          Right(s"${user.name}_${a}")
      }

    assertEquals(m(Tuple1(1)).run(User("user", false)).value, Some(Right("user_1")))
  }

  test("RpcMethod1.withAuthIf") {

    val m = RpcMethod1.withAuthIf[Id, User, "abc", Int, Unit, String](_.isAdmin) {
      case (user, a) =>
        Right(s"${user.name}_${a}")
    }

    assertEquals(m(Tuple(1)).run(User("user", true)).value, Some(Right("user_1")))
    assertEquals(m(Tuple(1)).run(User("user", false)).value, None)
  }

  test("RpcMethod1.withAuthIf without type parameters") {

    type AuthF = [x] =>> Auth[Id, User, x]

    val m: RpcMethod1[AuthF, "abc", Int, Unit, String] =
      RpcMethod1.withAuthIf((u: User) => u.isAdmin) {
        case (user, a) =>
          Right(s"${user.name}_${a}")
      }

    assertEquals(m(Tuple1(1)).run(User("user", true)).value, Some(Right("user_1")))
    assertEquals(m(Tuple1(1)).run(User("user", false)).value, None)
  }

}
