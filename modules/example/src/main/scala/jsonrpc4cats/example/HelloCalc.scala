package jsonrpc4cats.example

import cats.Applicative

object HelloCalc {

  def api[F[_]: Applicative] =
    HelloServer.api[F].extend(CalcServer.api[F])
}
