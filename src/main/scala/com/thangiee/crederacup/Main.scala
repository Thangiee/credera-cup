package com.thangiee.crederacup

import cats.data._
import cats.implicits._
import com.thangiee.crederacup.ApiReq._
import com.thangiee.crederacup.ApiResp._
import org.joda.time.format.PeriodFormatterBuilder

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global

object Main extends App {
  import GameServerOps._

  val practiceTrack= "wss://play.crederacup.com/season/2/PRACTICE"
  val authToken = "token"
  val numLaps = 50

  val gameServer = LiveGameServer(practiceTrack, authToken)

  val practiceSetup: Op[Unit] =
    for {
      _ <- set(SetTrackParams(numTurns = 10, numLaps = numLaps))
      _ <- set(SetVehicleParams(tire = "C",fuel = "A",spoilerAngle = 5.0,camberAngle = 2.5 ,airIntakeDiameter = 4.0))
      _ <- set(SetWeather(80, false))
    } yield ()

  val liveRunSetup: Op[Unit] = {
    import calculation._
    for {
      w <- getWeather
      _ <- set(SetVehicleParams(pickTire(w), pickFuel(w.temp), 5.0, 2.5, 4.0))
    } yield ()
  }

  val basicStrategy: RunStrategy = (prev, current) =>
    (prev, current) match {
      case (Some(p), c) =>
        import calculation._
        val fuelUse = delta((p, c))(_.fuel)
        val tireUse = delta((p, c))(_.tire)

        val changeTires = pickTire(c.currentWeather)
        val reFuel = pickFuel(c.currentWeather.temp)

        if (c.fuel <= fuelUse && c.tire <= tireUse + 1) set(Pit(changeTires, reFuel))
        else if (c.fuel <= fuelUse) set(Pit(fuel = reFuel))
        else if (c.tire <= tireUse) set(Pit(tire = changeTires))
        else                        continue
      case _ => continue
    }

  val raceProgram: Op[List[LapResult]] =
    for {
      _ <- practiceSetup
      _ <- beginRace
      laps <- runNLaps(numLaps, basicStrategy).runA(None)
      fLap <- waitLapFinish // finial lap
    } yield {
      laps :+ fLap
    }

  def report(lapResults: List[LapResult]): String = {
    import calculation._
    import com.github.nscala_time.time.Imports._

    val (fuelUsages, tireUsages) = lapResults
      .zip(lapResults.tail)
      .map(delta2(_)(_.fuel, _.tire))
      .unzip

    val avgFuelUse = avg(fuelUsages.filter(_ > 0))
    val avgTireUse = avg(tireUsages.filter(_ > 0))
    val avgPitTime = avg(lapResults.collect { case lap if lap.madePitStop => lap.PitTime })
    val avgLapTime = avg(lapResults.map(_.lapTime))

    val totalPitTime = lapResults.foldMap(_.PitTime)
    val totalTime = new Duration(((totalPitTime + lapResults.foldMap(_.lapTime)) * 1000).toInt)

    val timePrinter = new PeriodFormatterBuilder()
      .appendHours().appendSuffix("h ")
      .appendMinutes().appendSuffix("m ")
      .appendSeconds().appendSuffix("s ")
      .appendMillis()
      .toFormatter

    s"""
       |avg. fuel use: $avgFuelUse (%${avgFuelUse/18*100})
       |avg. tire use: $avgTireUse
       |avg. pit time: $avgPitTime
       |avg. lap time: $avgLapTime
       |total pit time: $totalPitTime
       |total time: ${timePrinter.print(totalTime.toPeriod())}
     """.stripMargin
  }

  import scala.concurrent.duration._
  Await.result(gameServer.run(raceProgram).value, 30.seconds) match {
    case Xor.Right(lapResults) =>
      println(report(lapResults))
      sys.exit(0)
    case Xor.Left(err)         =>
      println(err.getMessage)
      sys.exit(0)
  }
}
