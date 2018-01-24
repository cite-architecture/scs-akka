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
    "-Xmx4G"
)

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

resolvers += Resolver.jcenterRepo
resolvers += Resolver.bintrayRepo("neelsmith", "maven")
resolvers += Resolver.bintrayRepo("eumaeus", "maven")

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
    "edu.holycross.shot" %% "citerelations" % "2.0.1",
    "edu.holycross.shot" %% "ohco2" % "10.4.0",
    "edu.holycross.shot" %% "citebinaryimage" % "1.0.0" from "file:///cite/scala/unmanaged_jars/citebinaryimage_2.12-1.0.0.jar",
    "edu.holycross.shot" %% "scm" % "5.1.9" from "file:///cite/scala/unmanaged_jars/scm_2.12-5.1.9.jar",
    "edu.holycross.shot" %% "citeobj" % "5.0.1-m2" from "file:///cite/scala/unmanaged_jars/citeobj_2.12-5.0.1-m2.jar"
  )
}

assemblyJarName in assembly := "scs.jar"
test in assembly := {}

Revolver.settings
