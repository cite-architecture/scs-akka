import NativePackagerHelper._
enablePlugins(JavaAppPackaging)
enablePlugins(LauncherJarPlugin)

name := "cite-microservice"
organization := "edu.furman.classics"
version := "1.15.3"
scalaVersion := "2.12.4"

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
  val akkaVersion = "2.5.12"
  val akkaHttpVersion = "10.1.1"
  Seq(
    "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
    "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,
    "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "com.typesafe.akka" %% "akka-stream" % akkaVersion,
    "org.scalatest"     %% "scalatest" % scalaTestV % "test",
    "edu.holycross.shot.cite" %% "xcite" % "3.3.0",
    "edu.holycross.shot" %% "cex" % "6.2.1",
    "edu.holycross.shot" %% "citerelations" % "2.2.0",
    "edu.holycross.shot" %% "ohco2" % "10.8.1",
    "edu.holycross.shot" %% "scm" % "6.1.0",
    "edu.holycross.shot" %% "citebinaryimage" % "1.1.2",
    "edu.holycross.shot" %% "scm" % "6.1.0",
    "edu.holycross.shot" %% "citeobj" % "7.0.2",
    "edu.holycross.shot" %% "dse" % "3.1.0",
  )
}

assemblyJarName in assembly := "scs.jar"
test in assembly := {}

Revolver.settings
