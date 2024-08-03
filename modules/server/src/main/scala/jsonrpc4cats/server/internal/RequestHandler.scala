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
package internal

import cats.Applicative
import cats.Monad
import cats.MonadError
import cats.syntax.all.*

import jsonrpc4cats.*
import jsonrpc4cats.json.*

trait RequestHandler[F[_], A <: Coproduct, J] {
  def apply(req: String, srv: RpcServer[F, A], onError: RequestHandler.OnError[F, J]): F[Option[J]]
}

object RequestHandler {

  type OnError[F[_], J] = RpcErrorInfo[J] => F[Unit]

  given requestHandler[F[_], A <: Coproduct, J](using
    F: MonadError[F, Throwable],
    json: JsonFacade[J],
    parseRequest: JsonParser[J],
    decodeRequest: JsonDecoder[J, Request[J]],
    encodeError: JsonEncoder[J, ErrorResponse[ServerError]],
    eval: Eval[F, A, J]
  ): RequestHandler[F, A, J] =
    new RequestHandler {
      def apply(req: String, srv: RpcServer[F, A], onError: OnError[F, J]): F[Option[J]] = {

        def handleError(err: ServerError, id: Option[RequestId], ex: Option[Throwable] = None): F[Option[J]] =
          onError(RpcErrorInfo(err.code, err.message, None, req, ex)) *>
            F.pure(id.map(i => encodeError(ErrorResponse(err, i))))

        def handleRequest(j: J): F[Option[J]] = {
          val methodData =
            for {
              req <- decodeRequest(j).toRight((InvalidRequest, Some(NullId)))
              met <- srv(req.method).toRight((MethodNotFound, req.id))
            } yield (met, req.params, req.id)

          methodData match {
            case Left((err, id)) =>
              handleError(err, id)
            case Right((method, params, id)) =>
              eval(method, params, id, req, onError).attempt.flatMap {
                case Right(res) =>
                  F.pure(res)
                case Left(ex) =>
                  handleError(InternalError, id, Some(ex))
              }
          }
        }

        parseRequest(req).toRight(ParseError) match {
          case Left(err) =>
            handleError(err, Some(NullId))
          case Right(Left(Vector())) =>
            handleError(InvalidRequest, Some(NullId))
          case Right(Left(seq)) =>
            seq.traverse(handleRequest).map { res =>
              res.flatten match {
                case Vector() =>
                  None
                case xs =>
                  Some(json.fromValues(xs: _*))
              }
            }
          case Right(Right(obj)) =>
            handleRequest(obj)
        }

      }

    }

  trait Eval[F[_], A, J] {
    def apply(method: A, papams: J, id: Option[RequestId], req: String, onError: OnError[F, J]): F[Option[J]]
  }

  given cnilEval[F[_], J](using F: Applicative[F]): Eval[F, CNil, J] with {
    def apply(method: CNil, papams: J, id: Option[RequestId], req: String, onError: OnError[F, J]): F[Option[J]] =
      F.pure(None)
  }

  given coproductEval[F[_], H, T <: Coproduct, J](using
    hEval: Eval[F, H, J],
    tEval: Eval[F, T, J]
  ): Eval[F, H :+: T, J] with {
    def apply(method: H :+: T, params: J, id: Option[RequestId], req: String, onError: OnError[F, J]): F[Option[J]] =
      method match {
        case Inl(v) =>
          hEval(v, params, id, req, onError)
        case Inr(v) =>
          tEval(v, params, id, req, onError)
      }
  }

  given methodEval[F[_], K <: String & Singleton, P <: Product, E, R, J](using
    F: Monad[F],
    decodeParams: JsonDecoder[J, P],
    encodeResult: JsonEncoder[J, ResultResponse[R]],
    toRpcError: ToRpcError[J, E],
    encodeRpcError: JsonEncoder[J, ErrorResponse[RpcError[RpcErrorCode, J]]],
    encodeServerError: JsonEncoder[J, ErrorResponse[ServerError]]
  ): Eval[F, RpcMethod[F, K, P, E, R], J] with {
    def apply(
      method: RpcMethod[F, K, P, E, R],
      params: J,
      id: Option[RequestId],
      req: String,
      onError: OnError[F, J]
    ): F[Option[J]] = {

      def handleServerError(err: ServerError, id: Option[RequestId]): F[Option[J]] =
        onError(RpcErrorInfo(err.code, err.message, None, req, None)) *>
          F.pure(id.map(i => encodeServerError(ErrorResponse(err, i))))

      def handleRpcError(err: RpcError[RpcErrorCode, J], id: Option[RequestId]): F[Option[J]] =
        onError(RpcErrorInfo(err.code.toInt, err.message, err.data, req, None)) *>
          F.pure(id.map(i => encodeRpcError(ErrorResponse(err, i))))

      decodeParams(params).toRight(InvalidParams) match {
        case Left(err) =>
          handleServerError(err, id)
        case Right(par) =>
          method(par).flatMap {
            case Left(err) =>
              handleRpcError(toRpcError(err), id)
            case Right(res) =>
              F.pure(id.map(i => encodeResult(ResultResponse(res, i))))
          }
      }
    }
  }

}
