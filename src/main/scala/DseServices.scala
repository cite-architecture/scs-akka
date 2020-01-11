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

import edu.holycross.shot.cite._
import edu.holycross.shot.ohco2._
import edu.holycross.shot.citeobj._
import edu.holycross.shot.scm._
import edu.holycross.shot.dse._
import edu.holycross.shot.citebinaryimage._

case class DseRecordJson(urn: String, label: String, passage: String, imageroi:String, surface:String)
case class VectorOfDseRecordsJson(dseRecords: Vector[DseRecordJson])

  trait DseService extends Protocols {
    implicit val system: ActorSystem
    implicit def executor: ExecutionContextExecutor
    implicit val materializer: Materializer
    implicit val cexLibrary:CiteLibrary
    implicit val dseVector: DseVector
    implicit val collectionRepository:Option[CiteCollectionRepository]
    implicit val textRepository:Option[TextRepository]
    implicit def makeCiteObjectJson(objectFound:CiteObject): CiteObjectJson
    implicit def propertyUrnFromPropertyName(urn: Cite2Urn, propName: String): Cite2Urn

    def config: Config
    val logger: LoggingAdapter


    val dseModel: Cite2Urn = Cite2Urn("urn:cite2:cite:datamodels.v1:dsemodel")
    val passagePropString: String = "passage"
    val imagePropString: String = "imageroi"
    val surfacePropString: String = "surface"

    def hasDseModel(dseModel:Cite2Urn):Boolean = {
      dseVector.size > 0
    }

    def testDseService(urnString:String):Future[Either[String,String]] = {
      try {
        val u:Cite2Urn = Cite2Urn(urnString)
        val responseString:String = s"DSE Service is wired correctly: ${u}"
        Future.successful(Right(responseString))
      } catch {
        case e: Exception => {
          Future.successful(Left(s"${new IOException(e)}")) // was IOException
        }
      }
    }

    def dseRecordsForImage(imgUrnString: String): Future[Either[String,VectorOfDseRecordsJson]] = {
      try {
        val u: Cite2Urn = Cite2Urn(imgUrnString).dropExtensions
        val vodr: Vector[DseRecordJson] = { 
          if (hasDseModel(dseModel)) {
            dseVector
              .passages
              .filter(_.imageroi.dropExtensions == u)
              .map( rv => {
                  makeDseRecordJson(rv)
              }).toVector
          } else {
            Vector[DseRecordJson]()
          }
        }
        val returnVal: VectorOfDseRecordsJson = VectorOfDseRecordsJson(vodr)
        Future.successful(Right(returnVal))
      } catch {
        case e: Exception => {
          Future.successful(Left(s"${new IOException(e)}")) // was IOException
        }
      }
    }

 def dseRecordsForSurface(surfUrnString: String): Future[Either[String,VectorOfDseRecordsJson]] = {
     try {
        val u: Cite2Urn = Cite2Urn(surfUrnString).dropExtensions
        val vodr: Vector[DseRecordJson] = { 
         if (hasDseModel(dseModel)) {
            dseVector
              .passages
              .filter(_.surface == u)
              .map( rv => {
                  makeDseRecordJson(rv)
              }).toVector
          } else {
            Vector[DseRecordJson]()
          }
        }
        val returnVal: VectorOfDseRecordsJson = VectorOfDseRecordsJson(vodr)
        Future.successful(Right(returnVal))
      } catch {
        case e: Exception => {
          Future.successful(Left(s"${new IOException(e)}")) // was IOException
        }
      }
    }

    def dseRecordsForText(textUrnString: String): Future[Either[String,VectorOfDseRecordsJson]] = {
     try {
        val urn: CtsUrn = CtsUrn(textUrnString)
        val urnv: Vector[CtsUrn] = {
          textRepository match {
            case Some(tr) => {
              tr.corpus.validReff(urn)
            }
            case None => Vector(urn)
          }
        }
        val vodr: Vector[DseRecordJson] = { 
         if (hasDseModel(dseModel)) {
            dseVector
              .passages
              .filter(d => urnv.contains(d.passage))
              .map( rv => {
                  makeDseRecordJson(rv)
              }).toVector
          } else {
            Vector[DseRecordJson]()
          }
        }
        val returnVal: VectorOfDseRecordsJson = VectorOfDseRecordsJson(vodr)
        Future.successful(Right(returnVal))
      } catch {
        case e: Exception => {
          Future.successful(Left(s"${new IOException(e)}")) // was IOException
        }
      }
    }

def dseRecordsComprehensive(uv: Vector[Urn]):VectorOfDseRecordsJson = {
  try {
    val ctsUs: Vector[CtsUrn] = {
      uv.filter(u =>{
          u match {
            case CtsUrn(_) => true
            case _ => false
          } 
      }).map(u => u.asInstanceOf[CtsUrn]) 
    }
    // expand CtsUrns
    val expandedCtsUs: Vector[CtsUrn] = {
      textRepository match { 
        case Some(tr) => {
          val firstCut: Vector[CtsUrn] = (for (u <- ctsUs) yield {
            tr.corpus.validReff(u)
          }).flatten
          val originalVec: Vector[CtsUrn] = {
            ctsUs.map (_.asInstanceOf[CtsUrn])
          }
          (firstCut ++ originalVec).distinct
        }
        case None => ctsUs
      }
    }
    val cite2Us: Vector[Cite2Urn] = {
      uv.filter(u =>{
          u match {
            case Cite2Urn(_) => true
            case _ => false
          } 
      }).map(u => u.asInstanceOf[Cite2Urn]) 
    }
    val allUrns: Vector[Urn] = expandedCtsUs ++ cite2Us
    val dseRecs: Vector[DsePassage] = dseRecordsForUrnVector(allUrns).distinct
    val dseRecsJson: Vector[DseRecordJson] = {
      dseRecs.map(r => makeDseRecordJson(r))
    }

    val returnVal:VectorOfDseRecordsJson = VectorOfDseRecordsJson(dseRecsJson)
    returnVal 
  } catch {
    case e: Exception => {
        VectorOfDseRecordsJson(Vector())
    }
  }
}

// We aren't going to deliver DSE data unless it is all present in the library
def dseRecordsForUrnVector(uv: Vector[Urn]): Vector[DsePassage] = {
  val processedUrns: Vector[Urn] = uv.map( u => {
    u match {
      case citeu:Cite2Urn => {
        Vector(citeu.dropExtensions.asInstanceOf[Urn])
      }
      case ctsu:CtsUrn => {
        textRepository match {
          case Some(tr) => {
            tr.corpus.validReff(ctsu).map( _.asInstanceOf[Urn])
          }
          case None => Vector(u)
        }
      } 
      case _ => Vector(u)
    }
  }).flatten
  if (hasDseModel(dseModel)) {
    dseVector.passages.filter( dv => {
      (
        (processedUrns.contains(dv.surface.asInstanceOf[Urn])) |
        (processedUrns.contains(dv.passage.asInstanceOf[Urn])) |
        (processedUrns.contains(dv.imageroi.dropExtensions.asInstanceOf[Urn]))
      ) 
    }) 
  }  else {
    Vector[DsePassage]()
  }
}

def makeDseRecordJson(dr: DsePassage): DseRecordJson = {
    val urn: String = dr.urn.toString
    val label:String = dr.label 
    val passage:String = dr.passage.toString
    val imageroi:String = dr.imageroi.toString
    val surface:String = dr.surface.toString
    DseRecordJson(urn, label, passage, imageroi, surface)
}
   
 

  

 
 

}
