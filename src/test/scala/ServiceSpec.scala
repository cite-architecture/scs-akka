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

import edu.holycross.shot.ohco2._
import edu.holycross.shot.cite._
import edu.holycross.shot.citeobj._
import edu.holycross.shot.scm._


class ServiceSpec extends FlatSpec with Matchers with ScalatestRouteTest with CorsSupport with Service {

  override val corsAllowOrigins: List[String] = List("*","http://amphoreus.hpcc.uh.edu")

  override val corsAllowedHeaders: List[String] = List("Origin", "X-Requested-With", "Content-Type", "Accept", "Accept-Encoding", "Accept-Language", "Host", "Referer", "User-Agent")

  override val corsAllowCredentials: Boolean = true

  override val optionsCorsHeaders: List[HttpHeader] = List[HttpHeader](
    `Access-Control-Allow-Headers`(corsAllowedHeaders.mkString(", ")),
    `Access-Control-Max-Age`(60 * 60 * 24 * 20), // cache pre-flight response for 20 days
    `Access-Control-Allow-Credentials`(corsAllowCredentials)
  )

  override def testConfigSource = "akka.loglevel = WARNING"
  override def config = testConfig
  override val logger = Logging(system.eventStream, "edu.furman.akkascs")

  val textRepository:Option[TextRepository] = cexLibrary.textRepository 
  val collectionRepository:Option[CiteCollectionRepository] = cexLibrary.collectionRepository 
  val citableObjects:Option[Vector[CiteObject]] = collectionRepository match {
    case Some(cr) => Some(collectionRepository.get.citableObjects)
    case None => None
  }

  "The CiteMicroservice" should "respond to a path" in {
    Get(s"/ctsurn/urn:cts:greekLit:tlg0012.tlg001:1.1") ~> routes ~> check {
      val u:CtsUrn = CtsUrn("urn:cts:greekLit:tlg0012.tlg001:1.1")
      val urnStringReply:CtsUrnString = CtsUrnString(u.toString)
      status shouldBe OK
      contentType shouldBe `application/json`
      responseAs[CtsUrnString] shouldBe urnStringReply
    }
  }

  it should """respond to "/ctsurn/URN" and correctly use the xcite library to confirm a good URN" """ in {
    Get(s"/ctsurn/urn:cts:greekLit:tlg0012.tlg001:1.1") ~> routes ~> check {
      val u:CtsUrn = CtsUrn("urn:cts:greekLit:tlg0012.tlg001:1.1")
      val urnStringReply:CtsUrnString = CtsUrnString(u.toString)
      status shouldBe OK
      contentType shouldBe `application/json`
      responseAs[CtsUrnString] shouldBe urnStringReply
    }
  }

  it should """respond to "/ctsurn/URN" and correctly use the xcite library to confirm a bad URN" """ in {
    Get(s"/ctsurn/urn:cts:NOT-A-URN") ~> routes ~> check {
      logger.error(responseAs[String])
      status shouldBe BadRequest
      responseAs[String].length should be > 0
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

}
