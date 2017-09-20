package edu.furman.akkascs

import akka.event.NoLogging
import akka.event.Logging
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.ContentTypes._
import akka.http.scaladsl.model.{HttpResponse, HttpRequest}
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.stream.scaladsl.Flow
import org.scalatest._

import edu.holycross.shot.ohco2._
import edu.holycross.shot.cite._
import edu.holycross.shot.citeobj._
import edu.holycross.shot.scm._


class ServiceSpec extends FlatSpec with Matchers with ScalatestRouteTest with Service {
  override def testConfigSource = "akka.loglevel = WARNING"
  override def config = testConfig
  override val logger = Logging(system.eventStream, "my.nice.string")

  val ip1Info = IpInfo("8.8.8.8", Option("United States"), Option("Mountain View"), Option(37.386), Option(-122.0838))
  val ip2Info = IpInfo("8.8.4.4", Option("United States"), None, Option(38.0), Option(-97.0))
  val ipPairSummary = IpPairSummary(ip1Info, ip2Info)

  override lazy val ipApiConnectionFlow = Flow[HttpRequest].map { request =>
    if (request.uri.toString().endsWith(ip1Info.query))
      HttpResponse(status = OK, entity = marshal(ip1Info))
    else if(request.uri.toString().endsWith(ip2Info.query))
      HttpResponse(status = OK, entity = marshal(ip2Info))
    else
      HttpResponse(status = BadRequest, entity = marshal("Bad ip format"))
  }

  "Service" should "respond to single IP query" in {
    Get(s"/ip/${ip1Info.query}") ~> routes ~> check {
      status shouldBe OK
      contentType shouldBe `application/json`
      responseAs[IpInfo] shouldBe ip1Info
    }

    Get(s"/ip/${ip2Info.query}") ~> routes ~> check {
      status shouldBe OK
      contentType shouldBe `application/json`
      responseAs[IpInfo] shouldBe ip2Info
    }
  }

  it should "respond to IP pair query" in {
    Post(s"/ip", IpPairSummaryRequest(ip1Info.query, ip2Info.query)) ~> routes ~> check {
      status shouldBe OK
      contentType shouldBe `application/json`
      responseAs[IpPairSummary] shouldBe ipPairSummary
    }
  }

  it should "respond with bad request on incorrect IP format" in {
    Get("/ip/asdfg") ~> routes ~> check {
      status shouldBe BadRequest
      responseAs[String].length should be > 0
    }

    Post(s"/ip", IpPairSummaryRequest(ip1Info.query, "asdfg")) ~> routes ~> check {
      status shouldBe BadRequest
      responseAs[String].length should be > 0
    }

    Post(s"/ip", IpPairSummaryRequest("asdfg", ip1Info.query)) ~> routes ~> check {
      status shouldBe BadRequest
      responseAs[String].length should be > 0
    }
  }

  it should "respond to a second path" in {
    Get(s"/texts/million/urn:cts:greekLit:tlg0012.tlg001:1.1") ~> routes ~> check {
      val u:CtsUrn = CtsUrn("urn:cts:greekLit:tlg0012.tlg001:1.1")
      val urnStringReply:CtsUrnString = CtsUrnString(u.toString)
      status shouldBe OK
      contentType shouldBe `application/json`
      responseAs[CtsUrnString] shouldBe urnStringReply
    }
  }

  it should """respond to "texts" and correctly use the xcite library to confirm a good URN" """ in {
    Get(s"/texts/first/urn:cts:greekLit:tlg0012.tlg001:1.1") ~> routes ~> check {
      val u:CtsUrn = CtsUrn("urn:cts:greekLit:tlg0012.tlg001:1.1")
      val urnStringReply:CtsUrnString = CtsUrnString(u.toString)
      status shouldBe OK
      contentType shouldBe `application/json`
      responseAs[CtsUrnString] shouldBe urnStringReply
    }
  }

  it should """respond to "texts" and correctly use the xcite library to confirm a bad URN" """ in {
    Get(s"/texts/first/urn:cts:NOT-A-URN") ~> routes ~> check {
      logger.error(responseAs[String])
      status shouldBe BadRequest
      responseAs[String].length should be > 0
    }
  }

}
