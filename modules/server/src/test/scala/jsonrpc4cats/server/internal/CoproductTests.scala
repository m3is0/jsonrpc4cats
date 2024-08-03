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

package jsonrpc4cats.server.internal

import munit.FunSuite

class CoproductTests extends FunSuite {

  test("ExtendBy test") {

    import Coproduct.*

    type A = String :+: Int :+: CNil
    type B = Float :+: Boolean :+: CNil

    val a: A = Inr(Inl(1))
    val b: B = Inr(Inl(true))

    def extendByRight[L <: Coproduct, R <: Coproduct](l: L)(using
      ex: ExtendBy[L, R]
    ): Extend[L, R] =
      ex.right(l)

    def extendByLeft[L <: Coproduct, R <: Coproduct](r: R)(using
      ex: ExtendBy[L, R]
    ): Extend[L, R] =
      ex.left(r)

    type Res = String :+: Int :+: Float :+: Boolean :+: CNil

    assertEquals(extendByRight[A, B](a), Inr(Inl(1)): Res)
    assertEquals(extendByLeft[A, B](b), Inr(Inr(Inr(Inl(true)))): Res)
  }

}
