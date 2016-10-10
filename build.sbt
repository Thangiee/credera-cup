name := "credera-cup"

version := "1.0"

scalaVersion := "2.11.8"

resolvers += Resolver.jcenterRepo
libraryDependencies ++= Seq(
  "com.thangiee" %% "freasy-monad" % "0.4.0",
  "org.typelevel" %% "cats" % "0.7.2",
  "com.github.fommil" %% "spray-json-shapeless" % "1.3.0",
  "com.github.andyglow" %% "websocket-scala-client" % "0.1.2" % Compile,
  "com.lihaoyi" %% "scalarx" % "0.3.1",
  "com.github.nscala-time" %% "nscala-time" % "2.14.0"
)

addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)