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

package jsonrpc4cats.client
package internal

import cats.data.Const
import cats.data.NonEmptyList

import jsonrpc4cats.NumberId
import jsonrpc4cats.Request
import jsonrpc4cats.RequestId
import jsonrpc4cats.json.JsonEncoder

trait RequestGenerator[J, T <: NonEmptyTuple] {
  def apply(t: T): RequestGenerator.Out[J, T]
}

object RequestGenerator {

  type Out[J, T <: NonEmptyTuple] = (Signature[T], NonEmptyList[Request[J]])

  type Signature[T <: NonEmptyTuple] = GenerateSignature.Out[ZipWithIndex.Out[T]]

  // ---

  given reqGen[J, T <: NonEmptyTuple](using
    zipWithIndex: ZipWithIndex[T],
    generateSignature: GenerateSignature[ZipWithIndex.Out[T]],
    generateRequest: GenerateRequest[J, ZipWithIndex.Out[T]]
  ): RequestGenerator[J, T] with {
    def apply(t: T): Out[J, T] = {
      val zipped = zipWithIndex(t, 1)
      (generateSignature(zipped), generateRequest(zipped))
    }
  }

  // ZipWithIndex

  trait ZipWithIndex[T <: NonEmptyTuple] {
    def apply(t: T, i: Int): ZipWithIndex.Out[T]
  }

  object ZipWithIndex {

    type Out[T <: NonEmptyTuple] <: NonEmptyTuple =
      T match {
        case h *: EmptyTuple => (Int, h) *: EmptyTuple
        case h *: t => (Int, h) *: Out[t]
      }

    given htZwi[H, T <: NonEmptyTuple](using
      tZwi: ZipWithIndex[T]
    ): ZipWithIndex[H *: T] with {
      def apply(t: H *: T, i: Int) =
        t match {
          case h *: t => (i, h) *: tZwi(t, i + 1)
        }
    }

    given hZwi[H]: ZipWithIndex[H *: EmptyTuple] with {
      def apply(t: H *: EmptyTuple, i: Int) =
        t match {
          case h *: t => (i, h) *: EmptyTuple
        }
    }

  }

  // GenerateSignature

  trait GenerateSignature[C <: Tuple] {
    def apply(c: C): GenerateSignature.Out[C]
  }

  object GenerateSignature {

    type Out[C <: Tuple] <: Tuple =
      C match {
        case EmptyTuple => EmptyTuple
        case (Int, RpcNotification[?]) *: t => Out[t]
        case (Int, RpcRequest[?, r]) *: t => Const[RequestId, r] *: Out[t]
      }

    given emptyTuple: GenerateSignature[EmptyTuple] with {
      def apply(c: EmptyTuple) =
        c
    }

    given rpcNotification[H, T <: Tuple, P](using
      tGen: GenerateSignature[T]
    ): GenerateSignature[(Int, RpcNotification[P]) *: T] with {
      def apply(c: (Int, RpcNotification[P]) *: T) =
        c match {
          case _ *: t =>
            tGen(t)
        }
    }

    given rpcRequest[H, T <: Tuple, P, R](using
      tGen: GenerateSignature[T]
    ): GenerateSignature[(Int, RpcRequest[P, R]) *: T] with {
      def apply(c: (Int, RpcRequest[P, R]) *: T) =
        c match {
          case (i, _) *: t =>
            Const[RequestId, R](NumberId(i)) *: tGen(t)
        }
    }

  }

  // GenerateRequest

  trait GenerateRequest[J, T <: NonEmptyTuple] {
    def apply(t: T): NonEmptyList[Request[J]]
  }

  object GenerateRequest {

    trait ToRequest[J, A] {
      def apply(a: A): Request[J]
    }

    given rpcNotificationToRequest[J, P](using
      encodeParams: JsonEncoder[J, P]
    ): ToRequest[J, (Int, RpcNotification[P])] with {
      def apply(a: (Int, RpcNotification[P])): Request[J] =
        a match {
          case (i, RpcNotification(m, p)) =>
            Request(m, encodeParams(p), None)
        }
    }

    given rpcRequestToRequest[J, P, R](using
      encodeParams: JsonEncoder[J, P]
    ): ToRequest[J, (Int, RpcRequest[P, R])] with {
      def apply(a: (Int, RpcRequest[P, R])): Request[J] =
        a match {
          case (i, RpcRequest(m, p)) =>
            Request(m, encodeParams(p), Some(NumberId(i)))
        }
    }

    // ---

    given htGenerateRequest[J, H, T <: NonEmptyTuple](using
      hToRequest: ToRequest[J, H],
      tGenerate: GenerateRequest[J, T]
    ): GenerateRequest[J, H *: T] with {
      def apply(t: H *: T) =
        t match {
          case h *: t =>
            hToRequest(h) :: tGenerate(t)
        }
    }

    given hGenerateRequest[J, H](using
      hToRequest: ToRequest[J, H]
    ): GenerateRequest[J, H *: EmptyTuple] with {
      def apply(t: H *: EmptyTuple) =
        t match {
          case h *: EmptyTuple =>
            NonEmptyList.one(hToRequest(h))
        }
    }

  }

}
