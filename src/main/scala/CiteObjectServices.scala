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
case class CiteObjectJson(citeObject:Option[
      ( Map[String,String], 
        Map[String,String], 
        Vector[Map[String,String]] 
     )]
   )

case class VectorOfCiteObjectsJson(citeObjects:Vector[CiteObjectJson])

case class CiteCollectionDefJson(citeCollectionDef:Option[
      ( Map[String,String],
        Map[String,String],
        Map[String,String],
        Map[String,Option[String]],
        Map[String,Option[String]],
        Map[String,CitePropertyDefJson]
      )]
  )

case class VectorOfCiteCollectionDefsJson(citeCollectionDefs:Vector[CiteCollectionDefJson])

case class CitePropertyDefJson(
  citePropertyDef:Vector[
   (Map[String,String], Map[String,String], Map[String, String], Map[String, Vector[String]] )  
  ]
)
    

/*
case class CiteCatalogJson(
  citeCatalog:Vector[
    ( Map[String, String],
      Map[String, String],
      Map[String, Vector[ ( Map[String, String], Map[String,String], Map[String,String], Map[String,Vector[String]] ) ]],
      Map[String, String],
      Map[String, Option[String]],
      Map[String, Option[String]]
    )

  ]
)
*/

  trait CiteCollectionService extends Protocols {
    implicit val system: ActorSystem
    implicit def executor: ExecutionContextExecutor
    implicit val materializer: Materializer
    implicit val cexLibrary:CiteLibrary
    implicit val textRepository:Option[TextRepository]
    implicit val collectionRepository:Option[CiteCollectionRepository]
    implicit val citableObjects:Option[Vector[CiteObject]]

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

def fetchCiteCollectionDefJson(urnString: String): Future[Either[String, CiteCollectionDefJson]] = {
  try {
    val urn:Cite2Urn = Cite2Urn(urnString)
    val ccdj:CiteCollectionDefJson = fetchCiteCollectionDef(urn)
    Unmarshal(ccdj).to[CiteCollectionDefJson].map(Right(_))
  } catch {
    case e: Exception => {
      Future.successful(Left(s"${new IOException(e)}"))
    }
  }
}

def fetchVectorOfCiteCollectonDefsJson: Future[Either[String, VectorOfCiteCollectionDefsJson]] ={
  try {
    val collVec:VectorOfCiteCollectionDefsJson = VectorOfCiteCollectionDefsJson(
      collectionRepository.get.catalog.collections.map( cc => {
       val ccd:CiteCollectionDefJson = fetchCiteCollectionDef(cc.urn)
       ccd
    }))
    Unmarshal(collVec).to[VectorOfCiteCollectionDefsJson].map(Right(_))
  } catch {
    case e: Exception => {
      Future.successful(Left(s"${new IOException(e)}"))
    }
  }
}




//def fetchCiteCollectionDef(urnString: String): Future[Either[String, CiteCollectionDefJson]] = {
def fetchCiteCollectionDef(urn: Cite2Urn) : CiteCollectionDefJson = {
  try {
    val ccd:Option[CiteCollectionDef] = collectionRepository.get.collectionDefinition(urn.dropSelector)
    val urnReply:CiteCollectionDefJson = ccd match {
      case Some(d) => {
          val dUrn:String = d.urn.toString
          val dLabel:String = d.collectionLabel
          val dLicense:String = d.license
          val dOrdering:Option[String] = d.orderingProperty match {
            case Some(op) => Some(op.toString)
            case None => None
          }
          val dLabelling:Option[String] = d.labellingProperty match {
            case Some(op) => Some(op.toString)
            case None => None
          }
          val dPropertyDefs:CitePropertyDefJson = CitePropertyDefJson(d.propertyDefs.map( cpd => {
            val tempPd = (
              Map("urn" -> cpd.urn.toString),
              Map("label" -> cpd.label),
              Map("propertyType" -> cpd.propertyType.toString),
              Map("vocabularyList" -> cpd.vocabularyList)
            )
            tempPd
          }))
          CiteCollectionDefJson(
            Some(
              (
              Map("urn" -> dUrn),
              Map("collectionLabel" -> dLabel),
              Map("license" -> dLicense),
              Map("labellingProperty" -> dLabelling),
              Map("orderingProperty" -> dOrdering),
              Map("propertyDefs" -> dPropertyDefs)
              )
            )
          )
      }
      case None => CiteCollectionDefJson(None)
    }
    urnReply
    //Unmarshal(urnReply).to[CiteCollectionDefJson].map(Right(_))
  } catch {
    case e: Exception => {
      throw new ScsException(s"Exception creating collection definition for ${urn}.")
    }
  }

}

def fetchCiteObjectJson(urnString: String): Future[Either[String, VectorOfCiteObjectsJson]] = {
  try {
    val urn:Cite2Urn = Cite2Urn(urnString)
    val objectReply:VectorOfCiteObjectsJson = fetchCiteObjects(urn)
    Unmarshal(objectReply).to[VectorOfCiteObjectsJson].map(Right(_))
  } catch {
    case e: Exception => {
      Future.successful(Left(s"${new IOException(e)}"))
    }
  }

}

def fetchCiteObjects(urn: Cite2Urn):VectorOfCiteObjectsJson = {
  try {
    val vectorReply:VectorOfCiteObjectsJson = urn match {
      case u if (urn.objectComponentOption == None)  => {
        // Notional Collection
        val vo:Vector[CiteObject] = citableObjects.get.filter(_.urn ~~ urn)
        val replyVector:VectorOfCiteObjectsJson = VectorOfCiteObjectsJson(vo.map( v => {
          val c:CiteObjectJson = makeCiteObjectJson(v) 
          c
        }))
        replyVector
      }
      case u if (urn.isRange) => {
        logger.debug(s"\n\nhere\n\n")
        // Range
        val thisCollectionUrn:Cite2Urn = urn.dropSelector
        val thisCollectionDef:Option[CiteCollectionDef] = collectionRepository.get.catalog.collection(thisCollectionUrn) 
        thisCollectionDef match {
          case Some(tcd) => {
            tcd.orderingProperty match {
               case Some(op) => {
                  val collectionUrn:Cite2Urn = urn.dropSelector
                  val thisCollection:Vector[CiteObject] = citableObjects.get.filter(_.urn.dropSelector == collectionUrn)
        logger.debug(s"\n\nthisCollection.size = ${thisCollection.size}")
                  val rangeBegin:Cite2Urn = urn.rangeBeginUrn
                  val rangeEnd:Cite2Urn = urn.rangeEndUrn
        logger.debug(s"\n\nfrom ${rangeBegin} to ${rangeEnd}\n\n")
                  val beginSeq:Double = thisCollection.filter(_.urn == rangeBegin)(0).propertyValue(op).asInstanceOf[Double]
                  val endSeq:Double = thisCollection.filter(_.urn == rangeEnd)(0).propertyValue(op).asInstanceOf[Double]
        logger.debug(s"\n\nfrom ${beginSeq} to ${endSeq}\n\n")
                  val thisRange:Vector[CiteObject] = thisCollection.filter(
                       _.propertyValue(op).asInstanceOf[Double] >= beginSeq
                  ).filter(
                       _.propertyValue(op).asInstanceOf[Double] <= endSeq
                  )
        logger.debug(s"\n\nthisRange.size = ${thisRange.size}")
                  val replyVector:VectorOfCiteObjectsJson = VectorOfCiteObjectsJson(thisRange.map(v => {
                    val c:CiteObjectJson = makeCiteObjectJson(v)
                    c
                  } ))
                  replyVector
               } 
               case _ => throw new ScsException(s"${urn} is not an ordered collection.")
            }
          }
          case _ => VectorOfCiteObjectsJson(Vector(CiteObjectJson(None)))
        }
        //VectorOfCiteObjectsJson(Vector(CiteObjectJson(None)))
      }
      case u => {
        // Object
        val objectVector:Vector[CiteObject] = citableObjects.get.filter(_.urn == urn)
        objectVector.size match {
          case x if x > 0 => {
            val objectFound = objectVector(0)
            val objReply:CiteObjectJson = makeCiteObjectJson(objectFound)
            VectorOfCiteObjectsJson(Vector(objReply))
          }
          case _  => {
            val objReply = CiteObjectJson( None )
            VectorOfCiteObjectsJson(Vector(objReply))
          }
        }
      }
    }
    vectorReply
  } catch {
    case e: Exception => {
      throw new ScsException(s"Exception creating collection definition for ${urn}.")
    }
  }
}

def makeCiteObjectJson(objectFound:CiteObject):CiteObjectJson = {
    val objectUrn:Map[String,String] = Map("urn" -> objectFound.urn.toString)
    val objectLabel:Map[String,String] = Map("label" -> objectFound.label)
    val objectPropertiesVector:Vector[Map[String,String]] = objectFound.propertyList.map( p => {
      val m = Map("propertyUrn" -> p.urn.toString, "propertyValue" -> p.propertyValue.toString )
      m 
    })
    val objreply = CiteObjectJson( Some(objectUrn, objectLabel, objectPropertiesVector) )
    objreply
}


}
