package com.thangiee.crederacup

import cats.data.{Xor, XorT}
import cats.{Monad, RecursiveTailRecM}
import com.github.andyglow.websocket.{Uri, WebsocketClient, WebsocketHandler}
import rx._
import spray.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}
import scala.util.Success

case class LiveGameServer(trackUri: String, authToken: String)(implicit owner: Ctx.Owner) extends GameServerOps with Websocket.Interp[Future] {
  private val msgStream: Var[JsValue] = Var("{}".parseJson)

  val protocolHandler = new WebsocketHandler[String]() {
    def receive: PartialFunction[String, Unit] = {
      case resp =>
        Xor.catchNonFatal(resp.parseJson).fold(
          err => println(err.getMessage),
          jsValue => jsValue.asJsObject.fields.head match {
            case ("weatherResult" | "vehicleParams" | "trackParams" | "lapResult", json) =>
              if (json == msgStream.now) msgStream.recalc() else msgStream() = json
            case other => println(s"unexpected: $other")
          }
        )
    }
  }

  val cli: WebsocketClient[String] = WebsocketClient[String](
    Uri(trackUri),
    protocolHandler,
    headers = Map("X-Credera-Auth-Token" -> authToken)
  )
  val ws = cli.open()

  def lift[A](a: A): Future[A] = Future.successful(a)

  def send(msg: JsValue): Future[Unit] = {
    println(s"sending > ${msg.compactPrint}")
    Future(ws ! msg.compactPrint)
  }

  def receive: Future[JsValue] = {
    val p = Promise[JsValue]
    val obs = msgStream.triggerLater(p.complete(Success(msgStream.now)))
    p.future.map(v => {obs.kill(); v})
  }

  def run[A](op: Op[A])(implicit m: Monad[Future], r: RecursiveTailRecM[Future]): XorT[Future, Throwable, A] = XorT(run(op.value))
}
