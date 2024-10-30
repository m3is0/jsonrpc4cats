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

  final case class User(name: String, isAdmin: Boolean)

  test("RpcMethod.instance") {

    val m = RpcMethod.instance[Id, "abc", (Int, Int), Unit, Int] { (a, b) =>
      Right(a + b)
    }

    assertEquals(m((1, 2)), Right(3))
  }

  test("RpcMethod.instance without type parameters") {

    val m: RpcMethod[Id, "abc", (Int, Int), Unit, Int] =
      RpcMethod.instance { (a, b) =>
        Right(a + b)
      }

    assertEquals(m((1, 2)), Right(3))
  }

  test("RpcMethod.withAuth") {

    val m = RpcMethod.withAuth[Id, User, "abc", (Int, Int), Unit, String] {
      case (user, (a, b)) =>
        Right(s"${user.name}_${a}_${b}")
    }

    assertEquals(m((1, 2)).run(User("user", false)).value, Some(Right("user_1_2")))
  }

  test("RpcMethod.withAuth without type parameters") {

    type AuthF = [x] =>> Auth[Id, User, x]

    val m: RpcMethod[AuthF, "abc", (Int, Int), Unit, String] =
      RpcMethod.withAuth {
        case (user, (a, b)) =>
          Right(s"${user.name}_${a}_${b}")
      }

    assertEquals(m((1, 2)).run(User("user", false)).value, Some(Right("user_1_2")))
  }

  test("RpcMethod.withAuthIf") {

    val m = RpcMethod.withAuthIf[Id, User, "abc", (Int, Int), Unit, String](_.isAdmin) {
      case (user, (a, b)) =>
        Right(s"${user.name}_${a}_${b}")
    }

    assertEquals(m((1, 2)).run(User("user", true)).value, Some(Right("user_1_2")))
    assertEquals(m((1, 2)).run(User("user", false)).value, None)
  }

  test("RpcMethod.withAuthIf without type parameters") {

    type AuthF = [x] =>> Auth[Id, User, x]

    val m: RpcMethod[AuthF, "abc", (Int, Int), Unit, String] =
      RpcMethod.withAuthIf((u: User) => u.isAdmin) {
        case (user, (a, b)) =>
          Right(s"${user.name}_${a}_${b}")
      }

    assertEquals(m((1, 2)).run(User("user", true)).value, Some(Right("user_1_2")))
    assertEquals(m((1, 2)).run(User("user", false)).value, None)
  }

}
