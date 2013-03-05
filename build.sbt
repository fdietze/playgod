scalaVersion := "2.10.0"

libraryDependencies += "org.scala-lang" % "scala-swing" % "2.10.0"

libraryDependencies += "org.jbox2d" % "jbox2d-library" % "2.1.2.2"

libraryDependencies += "org.encog" % "encog-core" % "3.1.0"

LWJGLPlugin.lwjglSettings

// -XX:+UseConcMarkSweepGC -Djava.library.path=target/scala-2.10/resource_managed/main/lwjgl-resources/linux

scalacOptions ++= Seq("-optimize", "-unchecked", "-deprecation")
