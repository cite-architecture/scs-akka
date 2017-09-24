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
case class CatalogJson(citeCatalog:Vector[ Map[String,String] ] )
/*
case class CatalogJson(citeCatalog:Vector[(
  Map[String,String], 
  Map[String,String], 
  Map[String,String],
  Map[String,String],
  Map[String,String],
  Map[String,Option[String]],
  Map[String,Option[String]]   )])
  */


trait Ohco2Service extends Protocols {
  implicit val system: ActorSystem
  implicit def executor: ExecutionContextExecutor
  implicit val materializer: Materializer
  implicit val cexLibrary:CiteLibrary
  implicit val textRepository:Option[TextRepository]

  def config: Config
  val logger: LoggingAdapter

  def fetchCatalog:Future[Either[String,CatalogJson]] = {
    try {
      val cc:Vector[CatalogEntry] = textRepository.get.catalog.texts
      val v = cc.map(l => 
          Map("urn" -> l.urn.toString, 
           "citationScheme" -> l.citationScheme,
           "lang" -> l.lang,
           "groupName" -> l.groupName,
           "workTitle" -> l.workTitle,
           "versionLabel" -> l.versionLabel.toString,
           "exemplarLabel" -> l.exemplarLabel.toString
         ))
      val n:CatalogJson = CatalogJson(v) 
      Unmarshal(n).to[CatalogJson].map(Right(_))
    } catch {
      case e: Exception => {
        Future.successful(Left(s"${new IOException(e)}"))
      }
    }
  }

  def fetchOhco2Text(urnString: String): Future[Either[String,CorpusJson]] = {
  	 try {
      val urn:CtsUrn = CtsUrn(urnString)
      val c:Corpus = textRepository.get.corpus >= urn 
      val v:Vector[Map[String,String]] = c.nodes.map(l => Map("urn" -> l.urn.toString, "text" -> l.text))
      val n:CorpusJson = CorpusJson(v) 
      Unmarshal(n).to[CorpusJson].map(Right(_))
    } catch {
      case e: Exception => {
        Future.successful(Left(s"${new IOException(e)}"))
      }
    }
  }

  def testFetchOhco2Text(urnString: String): Future[Either[String,CorpusJson]] = {
     try {
      val urn:CtsUrn = CtsUrn(urnString)

      if (urn.passageComponentOption == None){ throw new ScsException("No passage component. This service will not return a whole text.") }

      urn.versionOption match {
        case Some(v) => {
          val c:Corpus = cexLibrary.textRepository.get.corpus >= urn 
          val v:Vector[Map[String,String]] = c.nodes.map(l => Map("urn" -> l.urn.toString, "text" -> l.text))
          val n:CorpusJson = CorpusJson(v) 
          Unmarshal(n).to[CorpusJson].map(Right(_))
        }
        case None => {
          val passageComp:String = urn.passageComponentOption.get
          val realUrns:Vector[CtsUrn] = textRepository.get.corpus.citedWorks        
          val corpora:Vector[Corpus] = realUrns.map(ru => textRepository.get.corpus >= CtsUrn(s"${ru.dropPassage}${passageComp}"))
          val assembledVector:Vector[Map[String,String]] = {
            (for (c <- corpora) yield {
              for (n <- c.nodes) yield {
                Map("urn" -> n.urn.toString, "text" -> n.text) 
              } 
            }).flatten
          }
          val n:CorpusJson = CorpusJson(assembledVector) 
          Unmarshal(n).to[CorpusJson].map(Right(_))
        }
      }      

    } catch {
      case e: Exception => {
        Future.successful(Left(s"${new IOException(e)}"))
      }
    }
  }

  def fetchFirstNode(urnString: String): Future[Either[String,CitableNodeJson]] = {
     try {
      val urn:CtsUrn = CtsUrn(urnString)
      val cn:CitableNode = textRepository.get.corpus.firstNode(urn)
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
              val filteredCorpus:Corpus = textRepository.get.corpus >= ctsU 
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
                  val ngh:StringHistogram = textRepository.get.corpus.ngramHisto(s,n,t,ignorePunctuation)
                  ngh
                }
                case None => {
                  val ngh:StringHistogram = textRepository.get.corpus.ngramHisto(n,t,ignorePunctuation)
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
