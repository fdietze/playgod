name := "playgod"

version := "0.1"

scalaVersion := "2.10.1"

resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

libraryDependencies ++= Seq("org.scala-lang" % "scala-swing" % "2.10.0",
                            "org.jbox2d" % "jbox2d-library" % "2.1.2.2",
                            "org.encog" % "encog-core" % "3.2.0-SNAPSHOT"
                            //"com.typesafe.akka" %% "akka-actor" % "2.1.2"
                            )

LWJGLPlugin.lwjglSettings



traceLevel in run := 8

// -XX:+UseConcMarkSweepGC -Djava.library.path=target/scala-2.10/resource_managed/main/lwjgl-resources/linux

//scalacOptions ++= Seq("-optimize", "-Xdisable-assertions")

scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature", "-Yinline-warnings")

scalacOptions ++= Seq("-language:reflectiveCalls")
