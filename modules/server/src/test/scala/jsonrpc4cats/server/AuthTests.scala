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

class AuthTests extends FunSuite {

  import Auth.*

  final case class User(role: String)

  test("Auth.allowAll test") {

    val action: Auth[Id, User, String] =
      allowAll(u => Id(u.role))

    assert(action.run(User("user")).value == Some("user"))

  }

  test("Auth.allowIf test") {

    val action: Auth[Id, User, String] =
      allowIf[User](_.role == "admin")(u => Id(u.role))

    assert(action.run(User("admin")).value == Some("admin"))
    assert(action.run(User("user")).value == None)

  }

  test("Auth.liftF test") {

    val action: Auth[Id, User, Int] =
      liftF(Id(1))

    assert(action.run(User("user")).value == Some(1))

  }

}
