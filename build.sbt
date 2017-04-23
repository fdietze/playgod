name := "playgod"

version := "0.1"
scalaVersion := "2.11.10"

libraryDependencies ++= Seq("org.scala-lang" % "scala-swing" % "2.11+",
                            "org.jbox2d" % "jbox2d-library" % "2.1.2.2",
                            "org.encog" % "encog-core" % "3.2.0"
                            )

LWJGLPlugin.lwjglSettings

libraryDependencies += "org.encog" % "encog-core" % "3.2.0"

LWJGLPlugin.lwjglSettings

// -XX:+UseConcMarkSweepGC -Djava.library.path=target/scala-2.10/resource_managed/main/lwjgl-resources/linux

scalacOptions ++= Seq("-optimize", "-unchecked", "-deprecation", "-feature", "-Yinline-warnings")
