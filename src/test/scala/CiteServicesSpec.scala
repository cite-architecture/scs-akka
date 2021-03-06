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

import akka.http.scaladsl.model.HttpHeader
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.headers.`Access-Control-Allow-Credentials`
import akka.http.scaladsl.model.headers.`Access-Control-Allow-Methods`
import akka.http.scaladsl.model.headers.`Access-Control-Allow-Origin`
import akka.http.scaladsl.model.headers.`Access-Control-Allow-Headers`
import akka.http.scaladsl.model.headers.`Access-Control-Max-Age`
import akka.http.scaladsl.model.headers.Origin
import akka.http.scaladsl.server.Directive0
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.MethodRejection
import akka.http.scaladsl.server.RejectionHandler



import java.net.{URI, URLDecoder, URLEncoder}

import spray.json._
import DefaultJsonProtocol._

import edu.holycross.shot.ohco2._
import edu.holycross.shot.cite._
import edu.holycross.shot.citeobj._
import edu.holycross.shot.scm._
import edu.holycross.shot.citerelation._


class CiteServiceSpec extends FlatSpec with Matchers with ScalatestRouteTest with Service with CorsSupport{

  override val corsAllowOrigins: List[String] = List("*","http://amphoreus.hpcc.uh.edu")

  override val corsAllowedHeaders: List[String] = List("Origin", "X-Requested-With", "Content-Type", "Accept", "Accept-Encoding", "Accept-Language", "Host", "Referer", "User-Agent")

  override val corsAllowCredentials: Boolean = true

  override val optionsCorsHeaders: List[HttpHeader] = List[HttpHeader](
    `Access-Control-Allow-Headers`(corsAllowedHeaders.mkString(", ")),
    `Access-Control-Max-Age`(60 * 60 * 24 * 20), // cache pre-flight response for 20 days
    `Access-Control-Allow-Credentials`(corsAllowCredentials)
  )
  
  override def testConfigSource = "akka.loglevel = INFO"
  override def config = testConfig
  override val logger = Logging(system.eventStream, "edu.furman.akkascs")

  val textRepository:Option[TextRepository] = cexLibrary.textRepository 
  val collectionRepository:Option[CiteCollectionRepository] = cexLibrary.collectionRepository 
  val citableObjects:Option[Vector[CiteObject]] = collectionRepository match {
    case Some(cr) => Some(collectionRepository.get.citableObjects)
    case None => None
  }

  "Ohco2Service" should "respond to single query" in {

    Get(s"/ctsurn/urn:cts:greekLit:tlg0012.tlg001:1.1") ~> routes ~> check {
      val u:CtsUrn = CtsUrn("urn:cts:greekLit:tlg0012.tlg001:1.1")
      val urnStringReply:CtsUrnString = CtsUrnString(u.toString)
      status shouldBe OK
      contentType shouldBe `application/json`
      responseAs[CtsUrnString] shouldBe urnStringReply
    }
  }

  it should """respond to "/libraryinfo" correctly""" in {
    Get(s"/libraryinfo") ~> routes ~> check {
      status shouldBe OK
      contentType shouldBe `application/json`
      val infoMap:Map[String,String] = Map("name" -> "scs-akka test library", "urn" -> "urn:cite2:cex:demo.2017_1:servicetest","license" -> "CC Share Alike. For details, see more info.")
      responseAs[Map[String,String]] shouldBe infoMap
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

  it should """respond to "/texts/label/URN" correctly """ in {
    Get(s"/texts/label/urn:cts:greekLit:tlg0016.tlg001.grc:1.1") ~> routes ~> check {
      status shouldBe OK
      responseAs[String] shouldBe "Herodotus, Histories (Greek, Godley, ed.) 1.1"
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
      r.ngramHisto(0)("count").toInt should equal (10)
    }  
  }

  it should """respond correctly to "/texts/ngram" with an "n" and "t" and "s" param """ in {
    val greekString:String = "Ἀχιλλεύς"
    val encodedString:String = URLEncoder.encode(greekString, "UTF-8")
    Get(s"/texts/ngram?s=${encodedString}&n=3&t=4") ~> routes ~> check {
      val r:NgramHistoJson  = responseAs[NgramHistoJson]
      status shouldBe OK
      contentType shouldBe `application/json`
      r.ngramHisto.size should equal (2)
      r.ngramHisto(0)("count").toInt should equal (29)
    }  
  }

  it should """respond correctly to "/texts/ngram/URN" with an "n" and "t" and "s" param """ in {
    val greekString:String = "Ἀχιλλεύς"
    val encodedString:String = URLEncoder.encode(greekString, "UTF-8")
    Get(s"/texts/ngram/urn:cts:greekLit:tlg0012.tlg001:1?s=${encodedString}&n=3&t=4") ~> routes ~> check {
      val r:NgramHistoJson  = responseAs[NgramHistoJson]
      status shouldBe OK
      contentType shouldBe `application/json`
      r.ngramHisto.size should equal (1)
      r.ngramHisto(0)("count").toInt should equal (6)
    }  
  }

  it should """respond correctly to "/texts/ngram/" as though it were "/texts/ngram" """ in {
    Get(s"/texts/ngram?n=8&t=9") ~> routes ~> check {
      val r:NgramHistoJson  = responseAs[NgramHistoJson]
      status shouldBe OK
      contentType shouldBe `application/json`
      r.ngramHisto.size should equal (1)
      r.ngramHisto(0)("count").toInt should equal (10)
    }  
  }

  it should """respond correctly to "/texts/ngram/URN" with an "n" and "t" param """ in {
    Get(s"/texts/ngram/urn:cts:greekLit:tlg0012.tlg001.perseus_grc2:1?n=3&t=5") ~> routes ~> check {
      val r:NgramHistoJson  = responseAs[NgramHistoJson]
      status shouldBe OK
      contentType shouldBe `application/json`
      r.ngramHisto.size should equal (1)
      r.ngramHisto(0)("count").toInt should equal (6)
    }  
  }

  it should """respond correctly to "/texts/ngram/urns?ng=STRING" correctly """ in {
    val greekString:String = "ἔπεα πτερόεντα προσηύδα"
    val encodedString:String = URLEncoder.encode(greekString, "UTF-8")
    Get(s"/texts/ngram/urns?ng=${encodedString}") ~> routes ~> check {
      val i:Int = 55
      val vu:ReffJson = responseAs[ReffJson]
      status shouldBe OK
      contentType shouldBe `application/json`
      vu.reff.size should equal(i)
    }
  }

  it should """respond correctly to "/texts/ngram/urns/URN?ng=STRING" correctly """ in {
    val greekString:String = "ἔπεα πτερόεντα προσηύδα"
    val encodedString:String = URLEncoder.encode(greekString, "UTF-8")
    Get(s"/texts/ngram/urns/urn:cts:greekLit:tlg0012.tlg001:1?ng=${encodedString}") ~> routes ~> check {
      val i:Int = 1 
      val vu:ReffJson = responseAs[ReffJson]
      status shouldBe OK
      contentType shouldBe `application/json`
      vu.reff.size should equal(i)
    }
  }

  it should """respond correctly to "/texts/token?t=STRING" correctly """ in {
    val tokenString:String = "Halicarnassos"
    val encodedString:String = URLEncoder.encode(tokenString, "UTF-8")
    Get(s"/texts/token?t=${encodedString}") ~> routes ~> check {
      val i:Int = 8 
      val vu:CorpusJson = responseAs[CorpusJson]
      status shouldBe OK
      contentType shouldBe `application/json`
      vu.citableNodes.size should equal(i)
    }
  }

  it should """respond correctly to "/texts/token?t=STRING&ignorePunctuation=false" correctly """ in {
    val tokenString:String = "Halicarnassos"
    val encodedString:String = URLEncoder.encode(tokenString, "UTF-8")
    Get(s"/texts/token?t=${encodedString}&ignorePunctuation=false") ~> routes ~> check {
      val i:Int = 2 
      val vu:CorpusJson = responseAs[CorpusJson]
      status shouldBe OK
      contentType shouldBe `application/json`
      vu.citableNodes.size should equal(i)
    }
  }

  it should """respond correctly to "/texts/CTS-URN/token?t=STRING" correctly """ in {
    val searchUrn:CtsUrn = CtsUrn("urn:cts:greekLit:tlg0016.tlg001.eng:1")
    val tokenString:String = "Halicarnassos"
    val encodedString:String = URLEncoder.encode(tokenString, "UTF-8")
    Get(s"/texts/token/${searchUrn}?t=${encodedString}") ~> routes ~> check {
      val i:Int = 3 
      val vu:CorpusJson = responseAs[CorpusJson]
      status shouldBe OK
      contentType shouldBe `application/json`
      vu.citableNodes.size should equal(i)
    }
  }

  it should """respond correctly to "/texts/CTS-URN/token?t=STRING&ignorePunctuation=false" correctly """ in {
    val searchUrn:CtsUrn = CtsUrn("urn:cts:greekLit:tlg0016.tlg001.eng:1")
    val tokenString:String = "Halicarnassos"
    val encodedString:String = URLEncoder.encode(tokenString, "UTF-8")
    Get(s"/texts/token/${searchUrn}?t=${encodedString}&ignorePunctuation=false") ~> routes ~> check {
      val i:Int = 1 
      val vu:CorpusJson = responseAs[CorpusJson]
      status shouldBe OK
      contentType shouldBe `application/json`
      vu.citableNodes.size should equal(i)
    }
  }

  it should "respond to a token search when there are tokenized exemplars in the mix" in {
    val searchUrn:CtsUrn = CtsUrn("urn:cts:greekLit:tlg0012.tlg001.perseus_grc2:1")
    val tokenString:String = "μῆνιν"
    val encodedString:String = URLEncoder.encode(tokenString, "UTF-8")
    Get(s"/texts/token/${searchUrn}?t=${encodedString}") ~> routes ~> check {
      val i:Int = 1 
      val vu:CorpusJson = responseAs[CorpusJson]
      status shouldBe OK
      contentType shouldBe `application/json`
      vu.citableNodes.size should equal(i)
    }
  }

  it should "respond to a token search when there are tokenized exemplars in the mix and we're searching for a single punctuation mark" in {
    val searchUrn:CtsUrn = CtsUrn("urn:cts:greekLit:tlg0012.tlg001.perseus_grc2.tokens:1.2")
    val tokenString:String = ","
    val encodedString:String = URLEncoder.encode(tokenString, "UTF-8")
    Get(s"/texts/token/${searchUrn}?t=${encodedString}&ignorePunctuation=false") ~> routes ~> check {
      val i:Int = 2 
      val vu:CorpusJson = responseAs[CorpusJson]
      status shouldBe OK
      contentType shouldBe `application/json`
      vu.citableNodes.size should equal(i)
    }
  }


it should """respond correctly to "/texts/tokens?t=STRING&t=STRING" correctly """ in {
    val tokenString1:String = "earth"
    val tokenString2:String = "water"
    val encodedString1:String = URLEncoder.encode(tokenString1, "UTF-8")
    val encodedString2:String = URLEncoder.encode(tokenString2, "UTF-8")
    Get(s"/texts/tokens?t=${encodedString1}&t=${encodedString2}") ~> routes ~> check {
      val i:Int = 25 
      val vu:CorpusJson = responseAs[CorpusJson]
      status shouldBe OK
      contentType shouldBe `application/json`
      vu.citableNodes.size should equal(i)
    }
  }

it should """respond correctly to "/texts/tokens/CTS-URN?t=STRING&t=STRING" correctly """ in {
    val searchUrn:CtsUrn = CtsUrn("urn:cts:greekLit:tlg0016.tlg001.eng:")
    val tokenString1:String = "earth"
    val tokenString2:String = "water"
    val encodedString1:String = URLEncoder.encode(tokenString1, "UTF-8")
    val encodedString2:String = URLEncoder.encode(tokenString2, "UTF-8")
    Get(s"/texts/tokens/${searchUrn}?t=${encodedString1}&t=${encodedString2}") ~> routes ~> check {
      val i:Int = 24 
      val vu:CorpusJson = responseAs[CorpusJson]
      status shouldBe OK
      contentType shouldBe `application/json`
      vu.citableNodes.size should equal(i)
    }
  }

  it should "respond to a token search with a specified distance between tokens" in {
    val searchUrn:CtsUrn = CtsUrn("urn:cts:greekLit:tlg0016.tlg001.eng:4")
    val tokenString1:String = "earth"
    val tokenString2:String = "water"
    val encodedString1:String = URLEncoder.encode(tokenString1, "UTF-8")
    val encodedString2:String = URLEncoder.encode(tokenString2, "UTF-8")
    val dist:Int = 3
    Get(s"/texts/tokens?dist=${dist}&t=${encodedString1}&t=${encodedString2}") ~> routes ~> check {
      val i:Int = 13 
      val vu:CorpusJson = responseAs[CorpusJson]
      status shouldBe OK
      contentType shouldBe `application/json`
      vu.citableNodes.size should equal(i)
    }

  }

  it should "return an empty vector for a token search with a specified distance between tokens" in {
    val searchUrn:CtsUrn = CtsUrn("urn:cts:greekLit:tlg0016.tlg001.eng:4")
    val tokenString1:String = "earth"
    val tokenString2:String = "water"
    val encodedString1:String = URLEncoder.encode(tokenString1, "UTF-8")
    val encodedString2:String = URLEncoder.encode(tokenString2, "UTF-8")
    val dist:Int = 1
    Get(s"/texts/tokens?dist=${dist}&t=${encodedString1}&t=${encodedString2}") ~> routes ~> check {
      val i:Int = 0 
      val vu:CorpusJson = responseAs[CorpusJson]
      status shouldBe OK
      contentType shouldBe `application/json`
      vu.citableNodes.size should equal(i)
    }

  }

  it should "respond to a token search with a specified distance between tokens, within a specified passage" in {
    val searchUrn:CtsUrn = CtsUrn("urn:cts:greekLit:tlg0016.tlg001.eng:4")
    val tokenString1:String = "earth"
    val tokenString2:String = "water"
    val encodedString1:String = URLEncoder.encode(tokenString1, "UTF-8")
    val encodedString2:String = URLEncoder.encode(tokenString2, "UTF-8")
    val dist:Int = 3
    Get(s"/texts/tokens/${searchUrn}?dist=${dist}&t=${encodedString1}&t=${encodedString2}") ~> routes ~> check {
      val i:Int = 2 
      val vu:CorpusJson = responseAs[CorpusJson]
      status shouldBe OK
      contentType shouldBe `application/json`
      vu.citableNodes.size should equal(i)
    }
  }

  it should """respond correctly to "/textcatalog """ in {
    Get(s"/textcatalog") ~> routes ~> check {
      val r:CatalogJson  = responseAs[CatalogJson]
      status shouldBe OK
      contentType shouldBe `application/json`
      r.ctsCatalog.size should equal (14)
    }  
  }

  it should """respond correctly to "/textcatalog/ as though it were /textcatalog """ in {
    Get(s"/textcatalog/") ~> routes ~> check {
      val r:CatalogJson  = responseAs[CatalogJson]
      status shouldBe OK
      contentType shouldBe `application/json`
      r.ctsCatalog.size should equal (14)
    }  
  }

  it should """respond correctly to "/texts as though it were /textcatalog """ in {
    Get(s"/texts") ~> routes ~> check {
      val r:CatalogJson  = responseAs[CatalogJson]
      status shouldBe OK
      contentType shouldBe `application/json`
      r.ctsCatalog.size should equal (14)
    }  
  }

  it should """respond correctly to "/texts/ as though it were /textcatalog """ in {
    Get(s"/texts/") ~> routes ~> check {
      val r:CatalogJson  = responseAs[CatalogJson]
      status shouldBe OK
      contentType shouldBe `application/json`
      r.ctsCatalog.size should equal (14)
    }  
  }

  it should """respond correctly to "/textcatalog/URN """ in {
    Get(s"/textcatalog/urn:cts:greekLit:tlg0012.tlg001:") ~> routes ~> check {
      val r:CatalogJson  = responseAs[CatalogJson]
      status shouldBe OK
      contentType shouldBe `application/json`
      r.ctsCatalog.size should equal (4)
    }  
  }

  /* CiteObjectServices */

   "CiteObjectService" should "respond to single query" in {

    Get(s"/cite2urn/urn:cite2:hmt:venAimg.2017a:1") ~> routes ~> check {
      val u:Cite2Urn = Cite2Urn("urn:cite2:hmt:venAimg.2017a:1")
      val urnStringReply:Cite2UrnString = Cite2UrnString(u.toString)
      status shouldBe OK
      contentType shouldBe `application/json`
      responseAs[Cite2UrnString] shouldBe urnStringReply
    }
  }

  it should """respond to "/cite2urn/URN" and correctly use the xcite library to confirm a good URN" """ in {
    Get(s"/cite2urn/urn:cite2:hmt:venAimg.2017a:1234") ~> routes ~> check {
      val u:Cite2Urn = Cite2Urn("urn:cite2:hmt:venAimg.2017a:1234")
      val urnStringReply:Cite2UrnString = Cite2UrnString(u.toString)
      status shouldBe OK
      contentType shouldBe `application/json`
      responseAs[Cite2UrnString] shouldBe urnStringReply
    }
  }

  it should """respond to "/cite2urn/URN" and correctly use the xcite library to confirm a bad URN" """ in {
    Get(s"/cite2urn/urn:cite2:NOT-A-URN") ~> routes ~> check {
      logger.error(responseAs[String])
      status shouldBe BadRequest
      responseAs[String].length should be > 0
    }
  }


  it should """respond to "/collections" with all collection definitions """ in {
    Get(s"/collections") ~> routes ~> check {
      status shouldBe OK
      contentType shouldBe `application/json`
      val r:VectorOfCiteCollectionDefsJson = responseAs[VectorOfCiteCollectionDefsJson]
      r.citeCollectionDefs.size should equal (11) 
    }
  }

  it should """respond to "/collections/COLLECTION-URN" with a single collection definition """ in {
    Get(s"/collections/urn:cite2:hmt:e4.v1:1r") ~> routes ~> check {
      status shouldBe OK
      contentType shouldBe `application/json`
      val r:CiteCollectionDefJson = responseAs[CiteCollectionDefJson]
      r.citeCollectionDef.citeCollectionInfo("urn") should equal ("urn:cite2:hmt:e4.v1:") 
    }
  }

  // /collections/hasobject/urn:cite2:hmt:e4.v1:1r
  it should """respond to "/collections/hasobject/OBJECT-URN" with "true" when the object is present """ in {
    Get(s"/collections/hasobject/urn:cite2:hmt:e4.v1:1r") ~> routes ~> check {
      status shouldBe OK
      val r:String = responseAs[String]
      r should equal ("true")
    }
  }

  it should """respond to "/collections/hasobject/OBJECT-URN" with "false" when the object is present """ in {
    Get(s"/collections/hasobject/urn:cite2:hmt:e4.v1:NOTPRESENT") ~> routes ~> check {
      status shouldBe OK
      val r:String = responseAs[String]
      r should equal ("false")
    }
  }

  it should """respond to "collections/objects?urn=URN1&urn=URN2&urn=URN3" with objects from three collections""" in {
    val u1:Cite2Urn = Cite2Urn("urn:cite2:cite:datamodels.v1:")
    val u2:Cite2Urn = Cite2Urn("urn:cite2:hmt:binaryimg.v1:")
    val u3:Cite2Urn = Cite2Urn("urn:cite2:hmt:e4.v1:")
    Get(s"/collections/objects?urn=${u1}&urn=${u2}&urn=${u3}") ~> routes ~> check {
      status shouldBe OK
      val r:VectorOfCiteObjectsJson = responseAs[VectorOfCiteObjectsJson]
      r.citeObjects.size should equal (24) 
    }
  }

  it should """respond to "/objects/nexturn/OBJECT-URN" with the next object's urn """ in {
    Get(s"/objects/nexturn/urn:cite2:hmt:e4.v1:1v") ~> routes ~> check {
      status shouldBe OK
      contentType shouldBe `application/json`
      val r:Cite2UrnString = responseAs[Cite2UrnString]
      r.urnString should equal ("urn:cite2:hmt:e4.v1:2r")
    }
  }

  it should """respond to "/objects/prevurn/OBJECT-URN" with the previous object's urn """ in {
    Get(s"/objects/prevurn/urn:cite2:hmt:e4.v1:1v") ~> routes ~> check {
      status shouldBe OK
      contentType shouldBe `application/json`
      val r:Cite2UrnString = responseAs[Cite2UrnString]
      r.urnString should equal ("urn:cite2:hmt:e4.v1:1r")
    }
  }

  it should """respond to "/objects/nexturn/RANGE-URN" with the next object's urn """ in {
    Get(s"/objects/nexturn/urn:cite2:hmt:e4.v1:1v-2r") ~> routes ~> check {
      status shouldBe OK
      contentType shouldBe `application/json`
      val r:Cite2UrnString = responseAs[Cite2UrnString]
      r.urnString should equal ("urn:cite2:hmt:e4.v1:2v")
    }
  }

  it should """respond to "/objects/prevurn/RANGE-URN" with the previous object's urn """ in {
    Get(s"/objects/prevurn/urn:cite2:hmt:e4.v1:1v-2r") ~> routes ~> check {
      status shouldBe OK
      contentType shouldBe `application/json`
      val r:Cite2UrnString = responseAs[Cite2UrnString]
      r.urnString should equal ("urn:cite2:hmt:e4.v1:1r")
    }
  }


  it should """respond to "/objects/COLLECTION-URN" with all objects in a collection """ in {
    Get(s"/objects/urn:cite2:hmt:e4.v1:") ~> routes ~> check {
      status shouldBe OK
      contentType shouldBe `application/json`
      val r:VectorOfCiteObjectsJson = responseAs[VectorOfCiteObjectsJson]
      r.citeObjects.size should equal (18) 
    }
  }

  it should """respond to "/objects/OBJECT-URN" with an object  """ in {
    Get(s"/objects/urn:cite2:hmt:e4.v1:5r") ~> routes ~> check {
      status shouldBe OK
      contentType shouldBe `application/json`
      val r:VectorOfCiteObjectsJson = responseAs[VectorOfCiteObjectsJson]
      r.citeObjects.size should equal (1) 
    }
  }

  it should """respond to "/objects/RANGE-URN" with objects defined by a range URN """ in {
    Get(s"/objects/urn:cite2:hmt:e4.v1:1r-2r") ~> routes ~> check {
      status shouldBe OK
      contentType shouldBe `application/json`
      val r:VectorOfCiteObjectsJson = responseAs[VectorOfCiteObjectsJson]
      r.citeObjects.size should equal (3) 
      r.citeObjects(0).citeObject.get("urn") should equal (Some("urn:cite2:hmt:e4.v1:1r"))
      r.citeObjects(2).citeObject.get("urn") should equal (Some("urn:cite2:hmt:e4.v1:2r"))
    }
  }

  it should """respond to "/objects/paged/COLLECTION-URN?offset=0&limit=10" with the first 10 objects in a collection """ in {
    Get(s"/objects/paged/urn:cite2:hmt:e4.v1:?offset=0&limit=10") ~> routes ~> check {
      status shouldBe OK
      contentType shouldBe `application/json`
      val r:VectorOfCiteObjectsJson = responseAs[VectorOfCiteObjectsJson]
      //logger.info(s"${r}")
      r.citeObjects(0).citeObject.get("urn") should equal (Some("urn:cite2:hmt:e4.v1:1r"))
      r.citeObjects(9).citeObject.get("urn") should equal (Some("urn:cite2:hmt:e4.v1:5v"))
      r.citeObjects.size should equal (10) 
    }
  }

  it should """respond to "/objects/paged/COLLECTION-URN?offset=5&limit=5" with the second 5 objects in a collection """ in {
    Get(s"/objects/paged/urn:cite2:hmt:e4.v1:?offset=5&limit=5") ~> routes ~> check {
      status shouldBe OK
      contentType shouldBe `application/json`
      val r:VectorOfCiteObjectsJson = responseAs[VectorOfCiteObjectsJson]
      r.citeObjects.size should equal (5) 
      r.citeObjects(0).citeObject.get("urn") should equal (Some("urn:cite2:hmt:e4.v1:3v"))
      r.citeObjects(4).citeObject.get("urn") should equal (Some("urn:cite2:hmt:e4.v1:5v"))
    }
  }


  it should """respond to "/objects/find/urnmatch?find=URN" by finding objects in all collections with URN as a property value""" in {
    Get(s"/objects/find/urnmatch?find=urn:cite2:hmt:msA.2017a:12v") ~> routes ~> check {
      status shouldBe OK
      contentType shouldBe `application/json`
      val r:VectorOfCiteObjectsJson = responseAs[VectorOfCiteObjectsJson]
      r.citeObjects.size should equal (3) 
    }
  }

  it should """respond to "/objects/find/urnmatch?find=URN&offset=0&limit=2" by finding the first two objects in all collections with URN as a property value""" in {
    Get(s"/objects/find/urnmatch?find=urn:cite2:hmt:msA.2017a:12v&offset=0&limit=2") ~> routes ~> check {
      status shouldBe OK
      contentType shouldBe `application/json`
      val r:VectorOfCiteObjectsJson = responseAs[VectorOfCiteObjectsJson]
      r.citeObjects.size should equal (2) 
    }
  }

  it should """also report stats for the above""" in {
    Get(s"/objects/find/urnmatch?find=urn:cite2:hmt:msA.2017a:12v&offset=0&limit=2") ~> routes ~> check {
      status shouldBe OK
      contentType shouldBe `application/json`
      val r:VectorOfCiteObjectsJson = responseAs[VectorOfCiteObjectsJson]
      r.citeObjects.size should equal (2) 
      val testMap:Map[String,String] = Map("total" -> "3","showing" -> "2")
      r.stats should equal (testMap)
    }
  }


  it should """respond to "/objects/find/urnmatch/COLLECTION-URN?find=URN" by finding objects in COLLECTION-URN with URN as a property value.""" in {
    Get(s"/objects/find/urnmatch/urn:cite2:hmt:textblock.2017a:?find=urn:cite2:hmt:msA.2017a:12v") ~> routes ~> check {
      status shouldBe OK
      contentType shouldBe `application/json`
      val r:VectorOfCiteObjectsJson = responseAs[VectorOfCiteObjectsJson]
      r.citeObjects.size should equal (1) 
    }
  }

  it should """respond to "/objects/find/urnmatch/COLLECTION-URN?find=URN&parameterurn=PROP-URN" by finding objects in COLLECTION-URN with URN as the value of property PROPERTY-URN""" in {
    Get(s"/objects/find/urnmatch/urn:cite2:hmt:textblock.2017a:?find=urn:cite2:hmt:vaimg.2017a:VA012VN_0514&parameterurn=urn:cite2:hmt:textblock.2017a.image:") ~> routes ~> check {
      status shouldBe OK
      contentType shouldBe `application/json`
      val r:VectorOfCiteObjectsJson = responseAs[VectorOfCiteObjectsJson]
      r.citeObjects.size should equal (1) 
    }

  }

  it should """respond to "/objects/find/urnmatch/COLLECTION-URN?find=URN&parameterurn=PROP-URN" even when the URN in data contains an extension""" in {
    Get(s"/objects/find/urnmatch/urn:cite2:hmt:textblock.2017a:?find=urn:cite2:hmt:vaimg.2017a:VA012VN_0514&parameterurn=urn:cite2:hmt:textblock.2017a.imageroi:") ~> routes ~> check {
      status shouldBe OK
      contentType shouldBe `application/json`
      val r:VectorOfCiteObjectsJson = responseAs[VectorOfCiteObjectsJson]
      r.citeObjects.size should equal (1) 
    }

  }

  it should """respond to "/objects/find/regexmatch" """ in pending
  it should """respond to "/objects/find/stringcontains" """ in pending
  it should """respond to "/objects/find/valueequals" with a string value """ in {
    Get(s"/objects/find/valueequals?propertyurn=urn:cite2:hmt:e4.v1.rv:&value=recto") ~> routes ~> check {
      status shouldBe OK
      contentType shouldBe `application/json`
      val r:VectorOfCiteObjectsJson = responseAs[VectorOfCiteObjectsJson]
      r.citeObjects.size should equal (9) 
    }
  }

  it should """respond to "/objects/find/valueequals" when given a type with a boolean value """ in {
    Get(s"/objects/find/valueequals?type=boolean&value=true") ~> routes ~> check {
      status shouldBe OK
      contentType shouldBe `application/json`
      val r:VectorOfCiteObjectsJson = responseAs[VectorOfCiteObjectsJson]
      r.citeObjects.size should equal (12) 
    }
  }

  it should """respond to "/objects/find/valueequals" when given a type with a boolean value and page the results """ in {
    Get(s"/objects/find/valueequals?type=boolean&value=true&offset=0&limit=2") ~> routes ~> check {
      status shouldBe OK
      contentType shouldBe `application/json`
      val r:VectorOfCiteObjectsJson = responseAs[VectorOfCiteObjectsJson]
      r.citeObjects.size should equal (2) 
    }
  }

  it should """respond to "/objects/find/valueequals" when given a type with a boolean value and page the results, even beyond the end of the set """ in {
    Get(s"/objects/find/valueequals?type=boolean&value=true&offset=10&limit=4") ~> routes ~> check {
      status shouldBe OK
      contentType shouldBe `application/json`
      val r:VectorOfCiteObjectsJson = responseAs[VectorOfCiteObjectsJson]
      r.citeObjects.size should equal (2) 
    }
  }

  it should """respond to "/objects/find/valueequals" when given a type with a string value """ in {
    Get(s"/objects/find/valueequals?type=string&value=recto") ~> routes ~> check {
      status shouldBe OK
      contentType shouldBe `application/json`
      val r:VectorOfCiteObjectsJson = responseAs[VectorOfCiteObjectsJson]
      r.citeObjects.size should equal (21) 
    }
  }
  it should """respond to "/objects/find/valueequals" with a numeric value """ in {
    Get(s"/objects/find/valueequals?propertyurn=urn:cite2:hmt:e4.v1.sequence:&value=3") ~> routes ~> check {
      status shouldBe OK
      contentType shouldBe `application/json`
      val r:VectorOfCiteObjectsJson = responseAs[VectorOfCiteObjectsJson]
      r.citeObjects.size should equal (1) 
      r.citeObjects(0).citeObject.get("urn") should equal (Some("urn:cite2:hmt:e4.v1:2r"))
    }
  }
  it should """respond to "/objects/find/valueequals" with a boolean value """ in {
    Get(s"/objects/find/valueequals?propertyurn=urn:cite2:hmt:e4.v1.fakeboolean:&value=true") ~> routes ~> check {
      status shouldBe OK
      contentType shouldBe `application/json`
      val r:VectorOfCiteObjectsJson = responseAs[VectorOfCiteObjectsJson]
      r.citeObjects.size should equal (12) 
    }
  }
  it should """respond to "/objects/find/valueequals" with a cts-urn value """ in {
    Get(s"/objects/find/valueequals?propertyurn=urn:cite2:hmt:msA.v1.text:&value=urn:cts:greekLit:tlg0012.tlg001.allen:1.1-1.25") ~> routes ~> check {
      status shouldBe OK
      contentType shouldBe `application/json`
      val r:VectorOfCiteObjectsJson = responseAs[VectorOfCiteObjectsJson]
      r.citeObjects.size should equal (2) 
    }
  }

  it should """respond to "/objects/find/valueequals" with a cite2-urn value """ in {
    Get(s"/objects/find/valueequals?propertyurn=urn:cite2:hmt:msA.v1.image:&value=urn:cite2:hmt:vaimg.2017a:VA012RN_0013") ~> routes ~> check {
      status shouldBe OK
      contentType shouldBe `application/json`
      val r:VectorOfCiteObjectsJson = responseAs[VectorOfCiteObjectsJson]
      r.citeObjects.size should equal (2) 
    }
  }

  it should """respond to "/objects/find/numeric" with operator "lt" (6) """ in {
    Get(s"/objects/find/numeric?n1=3&op=lt") ~> routes ~> check {
      status shouldBe OK
      contentType shouldBe `application/json`
      val r:VectorOfCiteObjectsJson = responseAs[VectorOfCiteObjectsJson]
      r.citeObjects.size should equal (6) 
    }
  }

  it should """respond to "/objects/find/numeric/CITE2-URN" with operator "lt" (2) """ in {
    Get(s"/objects/find/numeric/urn:cite2:hmt:e4.v1:?n1=3&op=lt") ~> routes ~> check {
      status shouldBe OK
      contentType shouldBe `application/json`
      val r:VectorOfCiteObjectsJson = responseAs[VectorOfCiteObjectsJson]
      r.citeObjects.size should equal (2) 
    }
  }

  it should """respond to "/objects/find/numeric" with operator "lt" and a property-URN (2) """ in {
    Get(s"/objects/find/numeric/urn:cite2:hmt:e4.v1:?n1=3&op=lt&propertyurn=urn:cite2:hmt:e4.v1.sequence:") ~> routes ~> check {
      status shouldBe OK
      contentType shouldBe `application/json`
      val r:VectorOfCiteObjectsJson = responseAs[VectorOfCiteObjectsJson]
      r.citeObjects.size should equal (2) 
    }
  }

  it should """respond to "/objects/find/numeric" with operator "lt" and a property-URN-with-no-property with an error """ in {
    Get(s"/objects/find/numeric/urn:cite2:hmt:e4.v1:?n1=3&op=lt&propertyurn=urn:cite2:hmt:e4.v1:") ~> routes ~> check {
      status shouldBe BadRequest
    }
  }

  it should """respond to "/objects/find/numeric" with operator "lteq" (9) """ in {
    Get(s"/objects/find/numeric?n1=3&op=lteq") ~> routes ~> check {
      status shouldBe OK
      contentType shouldBe `application/json`
      val r:VectorOfCiteObjectsJson = responseAs[VectorOfCiteObjectsJson]
      r.citeObjects.size should equal (9) 
    }
  }

  it should """respond to "/objects/find/numeric/CITE2-URN" with operator "lteq" (3) """ in {
    Get(s"/objects/find/numeric/urn:cite2:hmt:e4.v1:?n1=3&op=lteq") ~> routes ~> check {
      status shouldBe OK
      contentType shouldBe `application/json`
      val r:VectorOfCiteObjectsJson = responseAs[VectorOfCiteObjectsJson]
      r.citeObjects.size should equal (3) 
    }
  }

  it should """respond to "/objects/find/numeric" with operator "eq" (3) """ in {
    Get(s"/objects/find/numeric?n1=3&op=eq") ~> routes ~> check {
      status shouldBe OK
      contentType shouldBe `application/json`
      val r:VectorOfCiteObjectsJson = responseAs[VectorOfCiteObjectsJson]
      r.citeObjects.size should equal (3) 
    }
  }

  it should """respond to "/objects/find/numeric/CITE2-URN" with operator "eq" (1) """ in {
    Get(s"/objects/find/numeric/urn:cite2:hmt:e4.v1:?n1=3&op=eq") ~> routes ~> check {
      status shouldBe OK
      contentType shouldBe `application/json`
      val r:VectorOfCiteObjectsJson = responseAs[VectorOfCiteObjectsJson]
      r.citeObjects.size should equal (1) 
    }
  }

  it should """respond to "/objects/find/numeric" with operator "gt" (32) """ in {
    Get(s"/objects/find/numeric?n1=3&op=gt") ~> routes ~> check {
      status shouldBe OK
      contentType shouldBe `application/json`
      val r:VectorOfCiteObjectsJson = responseAs[VectorOfCiteObjectsJson]
      r.citeObjects.size should equal (33) 
    }
  }

  it should """respond to "/objects/find/numeric/CITE2-URN" with operator "gt" (15) """ in {
    Get(s"/objects/find/numeric/urn:cite2:hmt:e4.v1:?n1=3&op=gt") ~> routes ~> check {
      status shouldBe OK
      contentType shouldBe `application/json`
      val r:VectorOfCiteObjectsJson = responseAs[VectorOfCiteObjectsJson]
      r.citeObjects.size should equal (15) 
    }
  }

  it should """respond to "/objects/find/numeric" with operator "gteq" (35) """ in {
    Get(s"/objects/find/numeric?n1=3&op=gteq") ~> routes ~> check {
      status shouldBe OK
      contentType shouldBe `application/json`
      val r:VectorOfCiteObjectsJson = responseAs[VectorOfCiteObjectsJson]
      r.citeObjects.size should equal (36) 
    }
  }

  it should """respond to "/objects/find/numeric/CITE2-URN" with operator "gteq" (16) """ in {
    Get(s"/objects/find/numeric/urn:cite2:hmt:e4.v1:?n1=3&op=gteq") ~> routes ~> check {
      status shouldBe OK
      contentType shouldBe `application/json`
      val r:VectorOfCiteObjectsJson = responseAs[VectorOfCiteObjectsJson]
      r.citeObjects.size should equal (16) 
    }
  }

  it should """respond to "/objects/find/numeric" with operator "within" (2,4 == 9) """ in {
    Get(s"/objects/find/numeric?n1=2&op=within&n2=4") ~> routes ~> check {
      status shouldBe OK
      contentType shouldBe `application/json`
      val r:VectorOfCiteObjectsJson = responseAs[VectorOfCiteObjectsJson]
      r.citeObjects.size should equal (9) 
    }
  }

  it should """respond to "/objects/find/numeric/CITE2-URN" with operator "within" (2,4, == 3) """ in {
    Get(s"/objects/find/numeric/urn:cite2:hmt:e4.v1:?n1=2&op=within&n2=4") ~> routes ~> check {
      status shouldBe OK
      contentType shouldBe `application/json`
      val r:VectorOfCiteObjectsJson = responseAs[VectorOfCiteObjectsJson]
      r.citeObjects.size should equal (3) 
    }
  }

  it should """respond to "/datamodels"""" in {
    Get(s"/datamodels") ~> routes ~> check {
      status shouldBe OK
      contentType shouldBe `application/json`
      val dms:VectorOfDataModelsDefJson = responseAs[VectorOfDataModelsDefJson]
      dms.dataModels.size should equal (3) 
      assert(dms.dataModels(0).dataModel("collection") == "urn:cite2:hmt:binaryimg.v1:")
    }
  }
 

  it should """respond to "/image/URN?resolveImage=false" with a valid IIIF-API URL """ in {
    Get(s"/image/urn:cite2:hmt:vaimg.2017a:VA012RN_0013?resolveImage=false") ~> routes ~> check {
      status shouldBe OK
      val u:String = responseAs[String]
      u should equal ("http://www.homermultitext.org/iipsrv?IIIF=/project/homer/pyramidal/VenA/VA012RN_0013.tif/full/2000,/0/default.jpg")
    }
  }

  it should """respond to "/image/URN-with-ROI?resolveImage=false" with a valid IIIF-API URL """ in {
    Get(s"/image/urn:cite2:hmt:vaimg.2017a:VA012RN_0013@0.1,0.2,0.3,0.4?resolveImage=false") ~> routes ~> check {
      status shouldBe OK
      val u:String = responseAs[String]
      u should equal ("http://www.homermultitext.org/iipsrv?IIIF=/project/homer/pyramidal/VenA/VA012RN_0013.tif/pct:10.0,20.0,30.0,40.0/2000,/0/default.jpg")
    }
  }

  it should """respond to "/image/WIDTH/URN?resolveImage=false" with a valid IIIF-API URL """ in {
    Get(s"/image/200/urn:cite2:hmt:vaimg.2017a:VA012RN_0013?resolveImage=false") ~> routes ~> check {
      status shouldBe OK
      val u:String = responseAs[String]
      u should equal ("http://www.homermultitext.org/iipsrv?IIIF=/project/homer/pyramidal/VenA/VA012RN_0013.tif/full/200,/0/default.jpg")
    }
  }

  it should """respond to "/image/MAXWIDTH/MAXHEIGHT/URN-with-ROI?resolveImage=false" with a valid IIIF-API URL """ in {
    Get(s"/image/300/600/urn:cite2:hmt:vaimg.2017a:VA012RN_0013@?resolveImage=false") ~> routes ~> check {
      status shouldBe OK
      val u:String = responseAs[String]
      u should equal ("http://www.homermultitext.org/iipsrv?IIIF=/project/homer/pyramidal/VenA/VA012RN_0013.tif/full/!600,300/0/default.jpg")
    }
  }

  it should """respond to "/image/URN?resolveImage=false" when the object is not present with an error """ in {
    Get(s"/image/urn:cite2:hmt:vaimg.2017a:NOTPRESENT?resolveImage=false") ~> routes ~> check {
      status shouldBe BadRequest
    }
  }

  it should """respond to "/dse/recordsforimage/CITE2-URN"""" in {
    Get(s"/dse/recordsforimage/urn:cite2:hmt:vaimg.2017a:VA012RN_0013") ~> routes ~> check {
      status shouldBe OK
      contentType shouldBe `application/json`
      val r:VectorOfDseRecordsJson = responseAs[VectorOfDseRecordsJson]
      r.dseRecords.size should equal (20) 
    }
  }

  it should """respond to "/dse/recordsforsurface/CITE2-URN"""" in {
    Get(s"/dse/recordsforsurface/urn:cite2:hmt:msA.v1:12r") ~> routes ~> check {
      status shouldBe OK
      contentType shouldBe `application/json`
      val r:VectorOfDseRecordsJson = responseAs[VectorOfDseRecordsJson]
      r.dseRecords.size should equal (20) 
    }
  }

  it should """respond to "/dse/recordsfortext/CTS-URN" (leaf-node)""" in {
    Get(s"/dse/recordsfortext/urn:cts:greekLit:tlg0012.tlg001.msA:1.3") ~> routes ~> check {
      status shouldBe OK
      contentType shouldBe `application/json`
      val r:VectorOfDseRecordsJson = responseAs[VectorOfDseRecordsJson]
      r.dseRecords.size should equal (1) 
    }
  }

  it should """respond to "/dse/recordsfortext/CTS-URN" (range)""" in {
    Get(s"/dse/recordsfortext/urn:cts:greekLit:tlg0012.tlg001.msA:1.1-1.3") ~> routes ~> check {
      status shouldBe OK
      contentType shouldBe `application/json`
      val r:VectorOfDseRecordsJson = responseAs[VectorOfDseRecordsJson]
      r.dseRecords.size should equal (3) 
    }
  }

  it should """include DSE records when the ?dse= param is 'true' for object-requests""" in {
    Get(s"/objects/urn:cite2:hmt:vaimg.2017a:VA012RN_0013?dse=true") ~> routes ~> check {
      status shouldBe OK
      contentType shouldBe `application/json`
      val r:VectorOfCiteObjectsJson = responseAs[VectorOfCiteObjectsJson]
      r.citeObjects.size should equal (1) 
      r.dse.dseRecords.size should equal (20)
    }
  }

  it should """include DSE records when the ?dse= param is 'true' for Cts requests""" in {
    Get(s"/texts/urn:cts:greekLit:tlg0012.tlg001.msA:1.1?dse=true") ~> routes ~> check {
      status shouldBe OK
      contentType shouldBe `application/json`
      val r:CorpusJson = responseAs[CorpusJson]
      r.citableNodes.size should equal (1) 
      r.dse.dseRecords.size should equal (1)
    }
  }

  it should """respond to '/relations/verbs' by listing avaible CiteRelations""" in {
    Get(s"/relations/verbs") ~> routes ~> check {
      status shouldBe OK
      contentType shouldBe `application/json`
      val r:VectorOfCite2UrnsJson = responseAs[VectorOfCite2UrnsJson]
      r.cite2Urns.size should equal (5) 
    }
  }

  it should """respond to '/relations/verbmap' by listing avaible CiteRelations with labels""" in {
    Get(s"/relations/verbmap") ~> routes ~> check {
      status shouldBe OK
      contentType shouldBe `application/json`
      val r:ObjectLabelMapJson = responseAs[ObjectLabelMapJson]
      r.labelMap.size should equal (5) 
    }
  }


   it should """respond to '/relations/CITE2URN' by listing all relations""" in {
    Get(s"/relations/urn:cts:greekLit:tlg0012.tlg001.perseus_grc2:1.1") ~> routes ~> check {
      status shouldBe OK
      contentType shouldBe `application/json`
      val r:VectorOfCiteTriplesJson = responseAs[VectorOfCiteTriplesJson]
      r.citeTriples.size should equal (22)
      
    }
  }

  it should """respond to '/relations/CITE2URN' by listing all relations, using URN similarity""" in {
    Get(s"/relations/urn:cts:greekLit:tlg0012.tlg001:1.1") ~> routes ~> check {
      status shouldBe OK
      contentType shouldBe `application/json`
      val r:VectorOfCiteTriplesJson = responseAs[VectorOfCiteTriplesJson]
      r.citeTriples.size should equal (22)
      
    }
  }

  

  it should """respond to '/relations/CITE2URN' by listing all relations, ignoring sub-references on URNs""" in {
    Get(s"/relations/urn:cite2:hmt:vaimg.2017a:VA012RN_0013") ~> routes ~> check {
      status shouldBe OK
      contentType shouldBe `application/json`
      val r:VectorOfCiteTriplesJson = responseAs[VectorOfCiteTriplesJson]
      r.citeTriples.size should equal (4)
      
    }
  }

  it should """respond to '/relations/CTSURN' when the URN is a range by listing all relations""" in {
    Get(s"/relations/urn:cts:greekLit:tlg0012.tlg001.perseus_grc2:1.1-1.2") ~> routes ~> check {
      status shouldBe OK
      contentType shouldBe `application/json`
      val r:VectorOfCiteTriplesJson = responseAs[VectorOfCiteTriplesJson]
      r.citeTriples.size should equal (38)
    }
  }

/*
  it should """respond to '/relations/CTSURN' when the URN is containing-urn by listing all relations""" in {
    Get(s"/relations/urn:cts:greekLit:tlg0012.tlg001.perseus_grc2:1") ~> routes ~> check {
      status shouldBe OK
      contentType shouldBe `application/json`
      val r:VectorOfCiteTriplesJson = responseAs[VectorOfCiteTriplesJson]
      r.citeTriples.size should equal (17)
      
    }
  }
  */

  it should """respond to '/relations/CTSURN' with good urn awareness on both ends""" in {
    Get(s"/relations/urn:cts:greekLit:tlg0012.tlg001.perseus_grc2:1.4-1.5") ~> routes ~> check {
      status shouldBe OK
      contentType shouldBe `application/json`
      val r:VectorOfCiteTriplesJson = responseAs[VectorOfCiteTriplesJson]
      r.citeTriples.size should equal (30)
    }
  }

   it should """respond to '/relations/URN?filter=urn:cite2:cite:dseverbs.2017a:appearsOn' by listing all relations, filtered by relation-URN""" in {
    Get(s"/relations/urn:cts:greekLit:tlg0012.tlg001.perseus_grc2:1.1?filter=urn:cite2:cite:dseverbs.2017a:appearsOn") ~> routes ~> check {
      status shouldBe OK
      contentType shouldBe `application/json`
      val r:VectorOfCiteTriplesJson = responseAs[VectorOfCiteTriplesJson]
      r.citeTriples.size should equal (1)
    }
  }

   it should """respond to '/texts/URN?commentary=true' by including any commentaries as a VectorOfCiteTriplesJson""" in {
    Get(s"/texts/urn:cts:greekLit:tlg0012.tlg001.perseus_grc2:1.1-1.10?commentary=true") ~> routes ~> check {
      val r:CorpusJson  = responseAs[CorpusJson]
      status shouldBe OK
      contentType shouldBe `application/json`
      r.commentary.citeTriples.size should equal (91)
    }
  }

  it should """respond to '/texts/URN?commentary=true' when the URN is a work-level URN, with a correct number of commentary objects""" in {
    Get(s"/texts/urn:cts:greekLit:tlg0012.tlg001:1.1?commentary=true") ~> routes ~> check {
      val r:CorpusJson  = responseAs[CorpusJson]
      status shouldBe OK
      contentType shouldBe `application/json`
      r.commentary.citeTriples.size should equal (18)
    }
  }


   it should """respond to '/texts/URN?commentary=true' by including any commentaries as a VectorOfCiteTriplesJson when the URN is at the work-level""" in {
    Get(s"/texts/urn:cts:greekLit:tlg0012.tlg001:1.1-1.10?commentary=true") ~> routes ~> check {
      val r:CorpusJson  = responseAs[CorpusJson]
      status shouldBe OK
      contentType shouldBe `application/json`
      r.commentary.citeTriples.size should equal (91)
    }
  }

  it should """accept a vector of URNs for relation matching""" in {
    val urnVec:Vector[CtsUrn] = Vector(
        CtsUrn("urn:cts:greekLit:tlg0012.tlg001.perseus_grc2:1.1"),
        CtsUrn("urn:cts:greekLit:tlg0012.tlg001.perseus_grc2:1.2"),
        CtsUrn("urn:cts:greekLit:tlg0012.tlg001.perseus_grc2:1.3")
    )

    val osrs:Option[CiteRelationSet] = getRelations(urnVec,None)
    assert(osrs.get.relations.size == 37)
  }

  it should """accept a vector of URNs for relation matching, and use URN similarity for the match""" in {
    val urnVec:Vector[CtsUrn] = Vector(
        CtsUrn("urn:cts:greekLit:tlg0012.tlg001:1.1"),
        CtsUrn("urn:cts:greekLit:tlg0012.tlg001:1.2"),
        CtsUrn("urn:cts:greekLit:tlg0012.tlg001:1.3")
    )

    val osrs:Option[CiteRelationSet] = getRelations(urnVec,None)
    assert(osrs.get.relations.size == 37)
  }




}
