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
case class CorpusJson(citableNodes:Vector[Map[String, String]])
case class CitableNodeJson(citableNode:Map[String, String])
case class NgramHistoJson(ngramHisto:Vector[ (Map[String,String], Map[String,Int] ) ] )


trait Ohco2Service extends Protocols {
  implicit val system: ActorSystem
  implicit def executor: ExecutionContextExecutor
  implicit val materializer: Materializer
  implicit val cexLibrary:CiteLibrary

  def config: Config
  val logger: LoggingAdapter

  def fetchOhco2Text(urnString: String): Future[Either[String,CorpusJson]] = {
  	 try {
      val urn:CtsUrn = CtsUrn(urnString)
      val c:Corpus = cexLibrary.textRepository.get.corpus >= urn 
      val v:Vector[Map[String,String]] = c.nodes.map(l => Map("urn" -> l.urn.toString, "text" -> l.text))
      val n:CorpusJson = CorpusJson(v) 
      Unmarshal(n).to[CorpusJson].map(Right(_))
    } catch {
      case e: Exception => {
        Future.successful(Left(s"${new IOException(e)}"))
      }
    }
  }

  def fetchFirstNode(urnString: String): Future[Either[String,CitableNodeJson]] = {
     try {
      val urn:CtsUrn = CtsUrn(urnString)
      val cn:CitableNode = cexLibrary.textRepository.get.corpus.firstNode(urn)
      val cnj:Map[String,String] = Map("urn" -> cn.urn.toString, "text" -> cn.text)
      val n:CitableNodeJson = CitableNodeJson(cnj) 
      Unmarshal(n).to[CitableNodeJson].map(Right(_))
    } catch {
      case e: Exception => {
        Future.successful(Left(s"${new IOException(e)}"))
      }
    }
  }

  def fetchNgram(n:Int, t:Int, so:Option[String], urn:Option[String], ignorePunctuation:Boolean): Future[Either[String,NgramHistoJson]] = {
    try {
      val ngh:StringHistogram = {
        urn match {
           case Some(u) => {
              val ctsU = CtsUrn(u)
              val filteredCorpus:Corpus = cexLibrary.textRepository.get.corpus >= ctsU 
              so match {
                case Some(s) => {
                  val ngh:StringHistogram = filteredCorpus.ngramHisto(s,n,t,ignorePunctuation)
                  ngh
                }
                case None =>  {
                  val ngh:StringHistogram = filteredCorpus.ngramHisto(n,t,ignorePunctuation)
                  ngh
                }
              }
           }
           case None => {
              so match {
                case Some(s) => { 
                  val ngh:StringHistogram = cexLibrary.textRepository.get.corpus.ngramHisto(s,n,t,ignorePunctuation)
                  ngh
                }
                case None => {
                  val ngh:StringHistogram = cexLibrary.textRepository.get.corpus.ngramHisto(n,t,ignorePunctuation)
                  ngh
                }
              }
           }
        } 
      }

      val nghv:NgramHistoJson = NgramHistoJson(ngh.histogram.map(h => ( Map[String,String]("s" -> h.s.toString) , Map[String,Int]("count" -> h.count.toInt) ) ))
      Unmarshal(nghv).to[NgramHistoJson].map(Right(_)) 

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
