import NativePackagerHelper._
enablePlugins(JavaAppPackaging)
enablePlugins(LauncherJarPlugin)

name := "cite-microservice"
organization := "edu.furman.classics"
version := "2.0.0"
scalaVersion := "2.12.8"

fork in run := true

javaOptions in run ++= Seq(
    "-Xms256M",
	"-Xmn16M",
    "-Xmx4G"
)

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

resolvers += Resolver.jcenterRepo
resolvers += Resolver.bintrayRepo("neelsmith", "maven")
resolvers += Resolver.bintrayRepo("eumaeus", "maven")

libraryDependencies ++= {
  val scalaTestV  = "3.0.1"
  val akkaVersion = "2.6.1"
  val akkaHttpVersion = "10.1.11"
  Seq(
    "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
    "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
    "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,
    "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "com.typesafe.akka" %% "akka-stream" % akkaVersion,
    "org.scalatest"     %% "scalatest" % scalaTestV % "test",
    "edu.holycross.shot.cite" %% "xcite" % "4.2.0",
    "edu.holycross.shot" %% "cex" % "6.4.0",
    "edu.holycross.shot" %% "citerelations" % "2.6.0",
    "edu.holycross.shot" %% "ohco2" % "10.18.2",
    "edu.holycross.shot" %% "scm" % "7.2.0",
    "edu.holycross.shot" %% "citebinaryimage" % "3.1.1",
    "edu.holycross.shot" %% "citeobj" % "7.4.0",
    "edu.holycross.shot" %% "dse" % "6.0.4",
  )
}

assemblyJarName in assembly := "scs.jar"
test in assembly := {}

Revolver.settings
