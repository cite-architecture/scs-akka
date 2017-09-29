import NativePackagerHelper._
enablePlugins(JavaAppPackaging)
enablePlugins(LauncherJarPlugin)

name := "cite-microservice"
organization := "edu.furman.classics"
version := "0.1.2"
scalaVersion := "2.12.3"

fork in run := true

javaOptions in run ++= Seq(
    "-Xms256M", 
    "-Xmx4G", 
    "-XX:MaxPermSize=4096M", 
    "-XX:+UseConcMarkSweepGC"
)

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

resolvers += Resolver.jcenterRepo
resolvers += Resolver.bintrayRepo("neelsmith", "maven")

libraryDependencies ++= {
  val scalaTestV  = "3.0.1"
  val akkaVersion = "2.5.4"
  val akkaHttpVersion = "10.0.10"
  Seq(
    "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
    "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,
    "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "com.typesafe.akka" %% "akka-stream" % akkaVersion,
    "org.scalatest"     %% "scalatest" % scalaTestV % "test",

    "edu.holycross.shot.cite" %% "xcite" % "3.2.1",
    "edu.holycross.shot" %% "cex" % "6.1.0",
    "edu.holycross.shot" %% "ohco2" % "10.4.0",
    "edu.holycross.shot" %% "scm" % "5.1.6",
    "edu.holycross.shot" %% "citeobj" % "5.0.0"
  )
}


Revolver.settings
