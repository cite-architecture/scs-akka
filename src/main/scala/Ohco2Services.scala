package edu.furman.akkascs

import java.io.IOException
import java.io.File

import akka.actor.ActorSystem
import akka.event.{LoggingAdapter, Logging}
import akka.stream.{ActorMaterializer, Materializer}
import akka.stream.scaladsl.{Flow, Sink, Source}

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.unmarshalling.Unmarshal

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.math._

import spray.json.DefaultJsonProtocol

import edu.holycross.shot.ohco2._
import edu.holycross.shot.cite._
import edu.holycross.shot.citeobj._
import edu.holycross.shot.scm._

case class CtsUrnString(urnString: String)

case class CexLibraries(repos:Vector[CiteLibrary])

case class CitableNodesJson(citableNodes:Vector[Map[String, String]])


trait Ohco2Service extends Protocols {
  implicit val system: ActorSystem
  implicit def executor: ExecutionContextExecutor
  implicit val materializer: Materializer
  implicit val cexLibrary:CiteLibrary

  def config: Config
  val logger: LoggingAdapter

  def fetchOhco2Text(urnString: String): Future[Either[String,CitableNodesJson]] = {
  	 try {
      val urn:CtsUrn = CtsUrn(urnString)
      val c:Corpus = cexLibrary.textRepository.get.corpus >= urn 
      val v:Vector[Map[String,String]] = c.nodes.map(l => Map("urn" -> l.urn.toString, "text" -> l.text))
      val n:CitableNodesJson = CitableNodesJson(v) 
      Unmarshal(n).to[CitableNodesJson].map(Right(_))
    } catch {
      case e: Exception => {
        Future.successful(Left(s"${new IOException(e)}"))
      }
    }
  }

  def fetchCtsUrn(urnString: String): Future[Either[String, CtsUrnString]] = {
    try {
      val urn:CtsUrn = CtsUrn(urnString)
      val urnReply = CtsUrnString(urn.toString)
      Unmarshal(urnReply).to[CtsUrnString].map(Right(_))
    } catch {
      case e: Exception => {
        Future.successful(Left(s"${new IOException(e)}"))
      }
    }
  }

}
