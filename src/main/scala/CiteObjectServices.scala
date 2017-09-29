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

import java.net.{URI, URLDecoder, URLEncoder}

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.math._

import spray.json.DefaultJsonProtocol

import edu.holycross.shot.ohco2._
import edu.holycross.shot.cite._
import edu.holycross.shot.citeobj._
import edu.holycross.shot.scm._

case class Cite2UrnString(urnString: String)


  trait CiteCollectionService extends Protocols {
    implicit val system: ActorSystem
    implicit def executor: ExecutionContextExecutor
    implicit val materializer: Materializer
    implicit val cexLibrary:CiteLibrary
    implicit val textRepository:Option[TextRepository]

    def config: Config
    val logger: LoggingAdapter


def fetchCite2Urn(urnString: String): Future[Either[String, Cite2UrnString]] = {
  try {
    val urn:Cite2Urn = Cite2Urn(urnString)
    val urnReply = Cite2UrnString(urn.toString)
    Unmarshal(urnReply).to[Cite2UrnString].map(Right(_))
  } catch {
    case e: Exception => {
      Future.successful(Left(s"${new IOException(e)}"))
    }
  }
}


}
