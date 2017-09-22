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

import spray.json._
import DefaultJsonProtocol._

import edu.holycross.shot.ohco2._
import edu.holycross.shot.cite._
import edu.holycross.shot.citeobj._
import edu.holycross.shot.scm._


class Ohco2ServiceSpec extends FlatSpec with Matchers with ScalatestRouteTest with Service {
  override def testConfigSource = "akka.loglevel = INFO"
  override def config = testConfig
  override val logger = Logging(system.eventStream, "edu.furman.akkascs")


  "Ohco2Service" should "respond to single query" in {

    Get(s"/ctsurn/urn:cts:greekLit:tlg0012.tlg001:1.1") ~> routes ~> check {
      val u:CtsUrn = CtsUrn("urn:cts:greekLit:tlg0012.tlg001:1.1")
      val urnStringReply:CtsUrnString = CtsUrnString(u.toString)
      status shouldBe OK
      contentType shouldBe `application/json`
      responseAs[CtsUrnString] shouldBe urnStringReply
    }
  }

  it should """respond to "ctsurn" and correctly use the xcite library to confirm a good URN """ in {
    Get(s"/ctsurn/urn:cts:greekLit:tlg0012.tlg001:1.1") ~> routes ~> check {
      val u:CtsUrn = CtsUrn("urn:cts:greekLit:tlg0012.tlg001:1.1")
      val urnStringReply:CtsUrnString = CtsUrnString(u.toString)
      status shouldBe OK
      contentType shouldBe `application/json`
      responseAs[CtsUrnString] shouldBe urnStringReply
    }
  }

  it should """respond to "ctsurn" and correctly use the xcite library to confirm a bad URN """ in {
    Get(s"/ctsurn/urn:cts:NOT-A-URN") ~> routes ~> check {
      status shouldBe BadRequest
      responseAs[String].length should be > 0
    }
  }

  it should """respond to "texts/URN" correctly """ in {
    Get(s"/texts/urn:cts:greekLit:tlg0012.tlg001:1.1") ~> routes ~> check {
      val u:CtsUrn = CtsUrn("urn:cts:greekLit:tlg0012.tlg001:1.1")
      status shouldBe OK
      contentType shouldBe `application/json`
    }
  }

  it should """respond to "texts/first/URN" correctly """ in {
    Get(s"/texts/first/urn:cts:greekLit:tlg0012.tlg001.perseus_grc2:") ~> routes ~> check {
      val r:CitableNodeJson  = responseAs[CitableNodeJson]
      status shouldBe OK
      contentType shouldBe `application/json`
      r.citableNode("urn") should equal ("urn:cts:greekLit:tlg0012.tlg001.perseus_grc2:1.1")
    }
  }

  it should """respond correctly to "texts/ngram" with an "n" and "t" param """ in {
    Get(s"/texts/ngram?n=3&t=50") ~> routes ~> check {
      val r:NgramHistoJson  = responseAs[NgramHistoJson]
      status shouldBe OK
      contentType shouldBe `application/json`
      r.ngramHisto.size should equal (1)
      r.ngramHisto(0)._2("count") should equal (55)
    }  
  }

  it should """respond correctly to "texts/ngram/URN" with an "n" and "t" param """ in {
    Get(s"/texts/ngram/urn:cts:greekLit:tlg0012.tlg001.perseus_grc2:1?n=3&t=5") ~> routes ~> check {
      val r:NgramHistoJson  = responseAs[NgramHistoJson]
      status shouldBe OK
      contentType shouldBe `application/json`
      r.ngramHisto.size should equal (1)
      r.ngramHisto(0)._2("count") should equal (6)
    }  
  }


}
