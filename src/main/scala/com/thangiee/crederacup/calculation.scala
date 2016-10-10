package com.thangiee.crederacup

object calculation {

  def pickFuel(temp: Int): String = {
    if      (temp <= 70) "A"
    else if (temp <= 74) "A"
    else if (temp <= 79) "B"
    else                 "B"
  }

  def pickTire(weather: ApiResp.Weather): String = {
    if (weather.isRaining) {
      if (weather.temp <= 76) "A" else "B"
    } else {
      if (weather.temp <= 76) "C" else "D"
    }
  }

  def delta[A, B](a: (A, A))(p: A => B)(implicit num: Numeric[B]): B = {
    val (a1, a2) = a
    num.minus(p(a1), p(a2))
  }

  def delta2[A, B](a: (A, A))(p: A => B, q: A => B)(implicit num: Numeric[B]): (B, B) = (delta(a)(p), delta(a)(q))

  def avg[A](nums: Iterable[A])(implicit num: Numeric[A]): Double = num.toDouble(nums.sum) / nums.size

}
