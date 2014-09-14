name := "playgod"

version := "0.1"

scalaVersion := "2.11.2"

resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

libraryDependencies ++= Seq("org.scala-lang" % "scala-library-all" % "2.11.2",
                            "org.jbox2d" % "jbox2d-library" % "2.2.1.1",
                            "org.encog" % "encog-core" % "3.2.0"
                            //"com.typesafe.akka" %% "akka-actor" % "2.1.2"
                            )

LWJGLPlugin.lwjglSettings



traceLevel in run := 8

// -XX:+UseConcMarkSweepGC -Djava.library.path=target/scala-2.10/resource_managed/main/lwjgl-resources/linux

//scalacOptions ++= Seq("-optimize", "-Xdisable-assertions")

scalacOptions ++= Seq(
  "-encoding", "UTF-8",
  "-unchecked",
  "-deprecation",
  "-feature",
  "-Yinline", "-Yinline-warnings",
  "-language:_"
  ,"-Xdisable-assertions", "-optimize"
  )


scalacOptions ++= Seq("-language:reflectiveCalls")
