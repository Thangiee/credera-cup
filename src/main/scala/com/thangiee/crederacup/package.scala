package com.thangiee

import scala.language.implicitConversions

package object crederacup {

  implicit def liftToSome[A](a: A): Some[A] = Some(a)

}
