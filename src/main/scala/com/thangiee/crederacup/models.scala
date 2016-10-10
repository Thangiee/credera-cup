package com.thangiee.crederacup

sealed trait ApiResp
object ApiResp {
  case class Weather(temp: Int, isRaining: Boolean) extends ApiResp
  case class TrackParams(numTurns: Int, numLaps: Int) extends ApiResp
  case class Message(message: String) extends ApiResp

  case class VehicleParams(
    tire: String,
    fuel: String,
    spoilerAngle: Double,
    camberAngle: Double,
    airIntakeDiameter: Double
  ) extends ApiResp


  case class LapResult(
    lapNumber: Int,
    lapTime: Double,
    fuel: Double,
    tire: Double,
    madePitStop: Boolean,
    PitTime: Double,
    currentWeather: Weather,
    currentVehicle: VehicleParams
  ) extends ApiResp

  case class RaceResult(status: Int, trackParams: TrackParams, lapResults: Seq[LapResult], totalTime: Double) extends ApiResp
}

sealed trait ApiReq {
  val className: String = this.getClass.getSimpleName.replace("$", "")
}
object ApiReq {
  case class SetWeather(temp: Option[Int] = None, isRaining: Option[Boolean] = None) extends ApiReq
  case class SetTrackParams(numTurns: Option[Int] = None, numLaps: Option[Int] = None) extends ApiReq

  case class SetVehicleParams(
    tire: Option[String] = None,
    fuel: Option[String] = None,
    spoilerAngle: Option[Double] = None, // [0, 10]
    camberAngle: Option[Double] = None, // [0, 5]
    airIntakeDiameter: Option[Double] = None // [4, 8]
  ) extends ApiReq

  case class Pit(
    tire: Option[String] = None,
    fuel: Option[String] = None,
    spoilerAngle: Option[Double] = None,
    camberAngle: Option[Double] = None,
    airIntakeDiameter: Option[Double] = None
  ) extends ApiReq

  case object GetWeather extends ApiReq
  case object GetVehicleParams extends ApiReq
  case object GetTrackParams extends ApiReq
  case object BeginRace extends ApiReq
  case object Continue extends ApiReq
  case object Retire extends ApiReq
}
