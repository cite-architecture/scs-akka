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

  val textRepository:Option[TextRepository] = cexLibrary.textRepository 

  "Ohco2Service" should "respond to single query" in {

    Get(s"/ctsurn/urn:cts:greekLit:tlg0012.tlg001:1.1") ~> routes ~> check {
      val u:CtsUrn = CtsUrn("urn:cts:greekLit:tlg0012.tlg001:1.1")
      val urnStringReply:CtsUrnString = CtsUrnString(u.toString)
      status shouldBe OK
      contentType shouldBe `application/json`
      responseAs[CtsUrnString] shouldBe urnStringReply
    }
  }

  it should """respond to "/ctsurn" and correctly use the xcite library to confirm a good URN """ in {
    Get(s"/ctsurn/urn:cts:greekLit:tlg0012.tlg001:1.1") ~> routes ~> check {
      val u:CtsUrn = CtsUrn("urn:cts:greekLit:tlg0012.tlg001:1.1")
      val urnStringReply:CtsUrnString = CtsUrnString(u.toString)
      status shouldBe OK
      contentType shouldBe `application/json`
      responseAs[CtsUrnString] shouldBe urnStringReply
    }
  }

  it should """respond to "/ctsurn" and correctly use the xcite library to confirm a bad URN """ in {
    Get(s"/ctsurn/urn:cts:NOT-A-URN") ~> routes ~> check {
      status shouldBe BadRequest
      responseAs[String].length should be > 0
    }
  }

  it should """respond to "/texts/URN" correctly """ in {
    Get(s"/texts/urn:cts:greekLit:tlg0012.tlg001:1.1") ~> routes ~> check {
      val u:CtsUrn = CtsUrn("urn:cts:greekLit:tlg0012.tlg001:1.1")
      status shouldBe OK
      contentType shouldBe `application/json`
    }
  }

  it should """respond to "/texts/first/URN" correctly """ in {
    Get(s"/texts/first/urn:cts:greekLit:tlg0012.tlg001.perseus_grc2:") ~> routes ~> check {
      val r:CitableNodeJson  = responseAs[CitableNodeJson]
      status shouldBe OK
      contentType shouldBe `application/json`
      r.citableNode("urn") should equal ("urn:cts:greekLit:tlg0012.tlg001.perseus_grc2:1.1")
    }
  }

  it should """respond to "/texts/prev/URN" correctly """ in {
    Get(s"/texts/prev/urn:cts:greekLit:tlg0012.tlg001.perseus_grc2:1.10") ~> routes ~> check {
      val r:CorpusJson  = responseAs[CorpusJson]
      status shouldBe OK
      contentType shouldBe `application/json`
      r.citableNodes(0)("urn") should equal ("urn:cts:greekLit:tlg0012.tlg001.perseus_grc2:1.9")
    }
  }

  it should """respond to "/texts/next/URN" correctly """ in {
    Get(s"/texts/next/urn:cts:greekLit:tlg0012.tlg001.perseus_grc2:1.10") ~> routes ~> check {
      val r:CorpusJson  = responseAs[CorpusJson]
      status shouldBe OK
      contentType shouldBe `application/json`
      r.citableNodes(0)("urn") should equal ("urn:cts:greekLit:tlg0012.tlg001.perseus_grc2:1.11")
    }
  }

  it should """respond to "/texts/prev/URN" with an error if URN is at the work-level only.""" in {
    Get(s"/texts/prev/urn:cts:greekLit:tlg0012.tlg001:1.10") ~> routes ~> check {
      status shouldBe BadRequest
      responseAs[String].length should be > 0
    }
  }

  it should """respond to "/texts/next/URN" with an error if URN is at the work-level only.""" in {
    Get(s"/texts/next/urn:cts:greekLit:tlg0012.tlg001:1.10") ~> routes ~> check {
      status shouldBe BadRequest
      responseAs[String].length should be > 0
    }
  }

  it should """respond to "/texts/prevurn/URN" with an error if URN is at the work-level only.""" in {
    Get(s"/texts/prevurn/urn:cts:greekLit:tlg0012.tlg001:1.10") ~> routes ~> check {
      status shouldBe BadRequest
      responseAs[String].length should be > 0
    }
  }

  it should """respond to "/texts/nexturn/URN" with an error if URN is at the work-level only.""" in {
    Get(s"/texts/nexturn/urn:cts:greekLit:tlg0012.tlg001:1.10") ~> routes ~> check {
      status shouldBe BadRequest
      responseAs[String].length should be > 0
    }
  }

  it should """respond to "/texts/firsturn/URN" correctly """ in {
    Get(s"/texts/firsturn/urn:cts:greekLit:tlg0012.tlg001.perseus_grc2:") ~> routes ~> check {
      val u:CtsUrn = CtsUrn("urn:cts:greekLit:tlg0012.tlg001.perseus_grc2:1.1")
      val urnStringReply:CtsUrnString = CtsUrnString(u.toString)
      status shouldBe OK
      contentType shouldBe `application/json`
      responseAs[CtsUrnString] shouldBe urnStringReply
    }
  }

  it should """respond to "/texts/firsturn/URN" with the first URN of the work, even if URN specifies a passage """ in {
    Get(s"/texts/firsturn/urn:cts:greekLit:tlg0012.tlg001.perseus_grc2:1.20-1.23") ~> routes ~> check {
      val u:CtsUrn = CtsUrn("urn:cts:greekLit:tlg0012.tlg001.perseus_grc2:1.1")
      val urnStringReply:CtsUrnString = CtsUrnString(u.toString)
      status shouldBe OK
      contentType shouldBe `application/json`
      responseAs[CtsUrnString] shouldBe urnStringReply
    }
  }

  it should """respond to "/texts/nexturn/URN" correctly """ in {
    Get(s"/texts/nexturn/urn:cts:greekLit:tlg0012.tlg001.perseus_grc2:1.12") ~> routes ~> check {
      val u:CtsUrn = CtsUrn("urn:cts:greekLit:tlg0012.tlg001.perseus_grc2:1.13")
      val urnStringReply:CtsUrnString = CtsUrnString(u.toString)
      status shouldBe OK
      contentType shouldBe `application/json`
      responseAs[CtsUrnString] shouldBe urnStringReply
    }
  }

  it should """respond to "/texts/prevurn/URN" correctly """ in {
    Get(s"/texts/prevurn/urn:cts:greekLit:tlg0012.tlg001.perseus_grc2:1.12") ~> routes ~> check {
      val u:CtsUrn = CtsUrn("urn:cts:greekLit:tlg0012.tlg001.perseus_grc2:1.11")
      val urnStringReply:CtsUrnString = CtsUrnString(u.toString)
      status shouldBe OK
      contentType shouldBe `application/json`
      responseAs[CtsUrnString] shouldBe urnStringReply
    }
  }

  it should """respond to "/texts/prevurn/URN" with an empty string when URN is the first node """ in {
    Get(s"/texts/prevurn/urn:cts:greekLit:tlg0012.tlg001.perseus_grc2:1.1") ~> routes ~> check {
      val urnStringReply:CtsUrnString = CtsUrnString("")
      status shouldBe OK
      contentType shouldBe `application/json`
      responseAs[CtsUrnString] shouldBe urnStringReply
    }
  }

  it should """respond to "/texts/nexturn/URN" with an empty string when URN is the last node """ in {
    Get(s"/texts/nexturn/urn:cts:greekLit:tlg0012.tlg001.perseus_grc2:24.804") ~> routes ~> check {
      val urnStringReply:CtsUrnString = CtsUrnString("")
      status shouldBe OK
      contentType shouldBe `application/json`
      responseAs[CtsUrnString] shouldBe urnStringReply
    }
  }

  it should """respond correctly to "/texts/ngram" with an "n" and "t" param """ in {
    Get(s"/texts/ngram?n=8&t=9") ~> routes ~> check {
      val r:NgramHistoJson  = responseAs[NgramHistoJson]
      status shouldBe OK
      contentType shouldBe `application/json`
      r.ngramHisto.size should equal (1)
      r.ngramHisto(0)._2("count") should equal (10)
    }  
  }
  it should """respond correctly to "/texts/ngram/" as though it werre "/texts/ngram" """ in {
    Get(s"/texts/ngram?n=8&t=9") ~> routes ~> check {
      val r:NgramHistoJson  = responseAs[NgramHistoJson]
      status shouldBe OK
      contentType shouldBe `application/json`
      r.ngramHisto.size should equal (1)
      r.ngramHisto(0)._2("count") should equal (10)
    }  
  }

  it should """respond correctly to "/texts/ngram/URN" with an "n" and "t" param """ in {
    Get(s"/texts/ngram/urn:cts:greekLit:tlg0012.tlg001.perseus_grc2:1?n=3&t=5") ~> routes ~> check {
      val r:NgramHistoJson  = responseAs[NgramHistoJson]
      status shouldBe OK
      contentType shouldBe `application/json`
      r.ngramHisto.size should equal (1)
      r.ngramHisto(0)._2("count") should equal (6)
    }  
  }

  it should """respond correctly to "/textcatalog """ in {
    Get(s"/textcatalog") ~> routes ~> check {
      val r:CatalogJson  = responseAs[CatalogJson]
      status shouldBe OK
      contentType shouldBe `application/json`
      r.citeCatalog.size should equal (12)
    }  
  }

  it should """respond correctly to "/textcatalog/ as though it were /textcatalog """ in {
    Get(s"/textcatalog/") ~> routes ~> check {
      val r:CatalogJson  = responseAs[CatalogJson]
      status shouldBe OK
      contentType shouldBe `application/json`
      r.citeCatalog.size should equal (12)
    }  
  }

  it should """respond correctly to "/texts as though it were /textcatalog """ in {
    Get(s"/texts") ~> routes ~> check {
      val r:CatalogJson  = responseAs[CatalogJson]
      status shouldBe OK
      contentType shouldBe `application/json`
      r.citeCatalog.size should equal (12)
    }  
  }

  it should """respond correctly to "/texts/ as though it were /textcatalog """ in {
    Get(s"/texts/") ~> routes ~> check {
      val r:CatalogJson  = responseAs[CatalogJson]
      status shouldBe OK
      contentType shouldBe `application/json`
      r.citeCatalog.size should equal (12)
    }  
  }

  it should """respond correctly to "/textcatalog/URN """ in {
    Get(s"/textcatalog/urn:cts:greekLit:tlg0012.tlg001:") ~> routes ~> check {
      val r:CatalogJson  = responseAs[CatalogJson]
      status shouldBe OK
      contentType shouldBe `application/json`
      r.citeCatalog.size should equal (3)
    }  
  }


}
