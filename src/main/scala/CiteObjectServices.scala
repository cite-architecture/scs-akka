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

case class CiteCollectionDefJson(citeCollectionDef:Option[
      ( Map[String,String],
        Map[String,String],
        Map[String,String],
        Map[String,Option[String]],
        Map[String,Option[String]],
        Map[String,CitePropertyDefJson]
      )]
  )

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

def fetchCiteCollectionDef(urnString: String): Future[Either[String, CiteCollectionDefJson]] = {
  try {
    val urn:Cite2Urn = Cite2Urn(urnString)
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

    Unmarshal(urnReply).to[CiteCollectionDefJson].map(Right(_))
  } catch {
    case e: Exception => {
      Future.successful(Left(s"${new IOException(e)}"))
    }
  }

}

/*
def fetchCiteCatalog: Future[Either[String,CiteCatalogJson]] = {
  try {
    val cat:CiteCatalog = collectionRepository match {
      case Some(cr) => {
        cr.catalog
      }
      case None => { throw new ScsException("No collectionRepository present.") }
    }
    val catReply = cat.collections.map( c => {
        val urn:String = c.urn.toString
        val collectionLabel:String = c.collectionLabel
        val license:String = c.license
        val labellingProperty:Option[String] = c.labellingProperty match {
          case Some(l) => Some(l.toString)
          case _ => None
        }
        val orderingProperty:Option[String] = c.orderingProperty match {
          case Some(o) => Some(o.toString)
          case _ => None
        }
        val propDefs:Vector[ ( Map[String, String], Map[String,String], Map[String,String], Map[String,Vector[String]] ) ] = c.propertyDefs.map( pd => {
          val label:String = pd.label  
          val urn:String = pd.urn.toString
          val propertyType:String = pd.propertyType.toString
          val vocabularyList:Vector[String] = pd.vocabularyList
          val thisPropDef = Map(
            "label" -> label, 
            "urn" -> urn, 
            "propertyType" -> propertyType, 
            "vocabularyList" -> vocabularyList 
          )
          thisPropDef
        } )
        val thisCatalogEntry = Map(
          "urn" -> urn, 
          "collectionLabel" -> collectionLabel, 
          "license" -> license, 
          "labellingProperty" -> labellingProperty, 
          "orderingProperty" -> orderingProperty,
          "propertyDefs" -> propDefs
        )
        thisCatalogEntry
      }
    )
    Unmarshal(catReply).to[CiteCatalogJson].map(Right(_))
  } catch {
    case e: Exception => {
      Future.successful(Left(s"${new IOException(e)}"))
    }
  }
}
*/

def fetchCiteObject(urnString: String): Future[Either[String, CiteObjectJson]] = {
  try {
    val urn:Cite2Urn = Cite2Urn(urnString)
    //val objectVector:Vector[CiteObject] = collectionRepository.get ~~ urn
    val objectVector:Vector[CiteObject] = citableObjects.get.filter(_.urn == urn)
    val objectReply = {
      objectVector.size match {
        case x if x > 0 => {
          val objectFound = objectVector(0)
          val objectUrn:Map[String,String] = Map("urn" -> objectFound.urn.toString)
          val objectLabel:Map[String,String] = Map("label" -> objectFound.label)
          val objectPropertiesVector:Vector[Map[String,String]] = objectFound.propertyList.map( p => {
            val m = Map("propertyUrn" -> p.urn.toString, "propertyValue" -> p.propertyValue.toString )
            m 
          })
          val objreply = CiteObjectJson( Some(objectUrn, objectLabel, objectPropertiesVector) )
          objreply
        }
        case _  => {
          val objreply = CiteObjectJson( None )
          objreply
        }
      }
    }
    Unmarshal(objectReply).to[CiteObjectJson].map(Right(_))
  } catch {
    case e: Exception => {
      Future.successful(Left(s"${new IOException(e)}"))
    }
  }
}


}
