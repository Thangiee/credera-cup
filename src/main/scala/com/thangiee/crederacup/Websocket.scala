package com.thangiee.crederacup

import cats.data._
import cats.implicits._
import cats.free.Free
import com.thangiee.crederacup.ApiReq._
import com.thangiee.crederacup.ApiResp._
import fommil.sjs.FamilyFormats._
import freasymonad.cats.free
import spray.json._

@free trait Websocket {
  type IO[A] = Free[Adt, A]
  sealed trait Adt[A]

  def lift[A](a: A): IO[A]
  def send(msg: JsValue): IO[Unit]
  def receive: IO[JsValue]

  def sendAndReceive(msg: JsValue): IO[JsValue] = for {
    _   <- send(msg)
    res <- receive
  } yield res
}

trait GameServerOps {
  import Websocket.ops._
  type Op[A] = XorT[IO, Throwable, A]
  def Op[A](x: IO[Xor[Throwable, A]]): Op[A] = XorT[IO, Throwable, A](x)

  def pure[A](x: IO[A]): Op[A] = XorT.right[IO, Throwable, A](x)

  def pure[A](x: A): Op[A] = pure(lift(x))

  def getWeather: Op[Weather] = Op(sendAndReceive(encodeReq(GetWeather)).map(encodeResp[Weather]))

  def getVehicleParams: Op[VehicleParams] = Op(sendAndReceive(encodeReq(GetVehicleParams)).map(encodeResp[VehicleParams]))

  def getTrackParams: Op[TrackParams] = Op(sendAndReceive(encodeReq(GetTrackParams)).map(encodeResp[TrackParams]))

  def beginRace: Op[Unit] = set(BeginRace)

  def continue: Op[Unit] = set(Continue)

  def retire: Op[Unit] = set(Retire)

  def set(setterOp: ApiReq): Op[Unit] = pure(send(encodeReq(setterOp)))

  def waitLapFinish: Op[LapResult] = Op(receive.map(encodeResp[LapResult]))

  type PrevLapResult = Option[LapResult]
  type StateOp[A] = StateT[Op, PrevLapResult, A]
  type RunStrategy = (PrevLapResult, LapResult) => Op[Unit] // given the prev and current lap result, decide what to do next

  def runLap(runStrategy: RunStrategy): StateOp[LapResult] = {
    def ops(prevLap: PrevLapResult) = for {
      currentLap <- waitLapFinish
      _          <- runStrategy(prevLap, currentLap)
    } yield currentLap

    StateT[Op, PrevLapResult, LapResult](prev => ops(prev).map(current => (Option(current), current)))
  }

  def runNLaps(numLaps: Int, runStrategy: RunStrategy): StateOp[List[LapResult]] =
    List.fill(numLaps-1)(runLap(runStrategy)).sequence[StateOp, LapResult]

  def encodeReq[T <: ApiReq](req: T)(implicit fmt: JsonFormat[T]): JsValue = {
    val fields = req.toJson.asJsObject.fields + ("instruction" -> JsString(req.className))
    JsObject(fields).toJson
  }

  def encodeResp[T <: ApiResp](json: JsValue)(implicit fmt: JsonFormat[T]): Xor[Throwable, T] = Xor.catchNonFatal {
    json.convertTo[T]
  }
}
object GameServerOps extends GameServerOps