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

/*
 * Based on https://github.com/milessabin/shapeless/blob/ecd9e7c996d18088060d68df34489060bf4bce25/core/shared/src/main/scala/shapeless/coproduct.scala
 * Copyright (c) 2013-14 Miles Sabin
 */

package jsonrpc4cats.server.internal

sealed trait Coproduct extends Product with Serializable
sealed trait :+:[+H, +T <: Coproduct] extends Coproduct
final case class Inl[+H, +T <: Coproduct](h: H) extends :+:[H, T]
final case class Inr[+H, +T <: Coproduct](t: T) extends :+:[H, T]
sealed trait CNil extends Coproduct

object Coproduct {

  type Extend[L <: Coproduct, R <: Coproduct] <: Coproduct =
    (L, R) match {
      case (CNil, h1 :+: t1) => h1 :+: t1
      case (h0 :+: t0, h1 :+: t1) => h0 :+: Extend[t0, h1 :+: t1]
    }

  trait ExtendBy[L <: Coproduct, R <: Coproduct] {
    def right(l: L): Extend[L, R]
    def left(r: R): Extend[L, R]
  }

  given ext1[H0, H1, T1 <: Coproduct]: ExtendBy[H0 :+: CNil, H1 :+: T1] with {
    def right(l: H0 :+: CNil) =
      (l: @unchecked) match {
        case Inl(v) => Inl(v)
      }

    def left(r: H1 :+: T1) =
      Inr(r)
  }

  given ext2[HH, H0, T0 <: Coproduct, H1, T1 <: Coproduct](using
      e: ExtendBy[H0 :+: T0, H1 :+: T1]
  ): ExtendBy[HH :+: H0 :+: T0, H1 :+: T1] with {
    def right(l: HH :+: H0 :+: T0) =
      l match {
        case Inl(v) => Inl(v)
        case Inr(v) => Inr(e.right(v))
      }

    def left(r: H1 :+: T1) =
      Inr(e.left(r))
  }

}
