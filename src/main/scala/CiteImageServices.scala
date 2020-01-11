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
import edu.holycross.shot.citeobj._
import edu.holycross.shot.scm._
import edu.holycross.shot.citebinaryimage._

case class ServiceUrlString(urlString: String)

  trait CiteImageService extends Protocols {
    implicit val system: ActorSystem
    implicit def executor: ExecutionContextExecutor
    implicit val materializer: Materializer
    implicit val collectionRepository:Option[CiteCollectionRepository]
    implicit val cexLibrary:CiteLibrary

    def config: Config
    val logger: LoggingAdapter

    val binaryImageModel:Cite2Urn = Cite2Urn("urn:cite2:cite:datamodels.v1:binaryimg")
    val binaryImageCollection:Cite2Urn = Cite2Urn("urn:cite2:hmt:binaryimg.v1:")
    val binaryImagePathProp:Cite2Urn = Cite2Urn("urn:cite2:hmt:binaryimg.v1.path:")
    val binaryImageUrlProp:Cite2Urn = Cite2Urn("urn:cite2:hmt:binaryimg.v1.url:")

    val protocolPropertyName:String = "protocol"
    val iiifApiProtocolString:String = "iiifApi"

    def iiifApiUrl(
      urn:Cite2Urn, 
      width:Option[Int] = None, 
      maxWidth:Option[Int] = None, 
      maxHeight:Option[Int] = None
    ):Future[Either[String,String]] = {

      try {
        val serverData:Map[String,String] = gatherInfo(urn)
        val baseUrl = serverData("baseUrl")
        val serverPath = serverData("serverPath")
        val iifService:IIIFApi = IIIFApi(baseUrl, serverPath) 
        val responseString:String = iifService.serviceRequest(urn, width = width, maxWidth = maxWidth, maxHeight = maxHeight)
        Future.successful(Right(responseString))
      } catch {
        case e: Exception => {
          Future.successful(Left(s"${new IOException(e)}")) // was IOException
        }
      }
    }

    def iiifApiResolver(
      urn:Cite2Urn, 
      width:Option[Int] = None, 
      maxWidth:Option[Int] = None, 
      maxHeight:Option[Int] = None
    ):String = {

      try {
        val serverData:Map[String,String] = gatherInfo(urn)
        val baseUrl = serverData("baseUrl")
        val serverPath = serverData("serverPath")
        val iifService:IIIFApi = IIIFApi(baseUrl, serverPath) 
        val responseString:String = iifService.serviceRequest(urn, width = width, maxWidth = maxWidth, maxHeight = maxHeight)
        responseString
      } catch {
        case e: Exception => {
          e.toString
        }
      }
    }

    def gatherInfo(urn:Cite2Urn):Map[String,String] = {
      try {
        val collectionUrn:Cite2Urn = urn.dropProperty.dropSelector 

        // Check that this object even exists in data
        val thisObjectExists:CiteObject = collectionRepository.get.citableObject(urn)

        // Check that URN has an object-selector
        if (urn.objectComponentOption == None){
          logger.info(s"${urn} does not specify an object.")
          throw new ScsException(s"${urn} does not specify an object.")
        }
        // Check for no objects at all
        if (collectionRepository == None) {
          logger.info(s"No collection objects in library.")
          throw new ScsException(s"No collection objects in library.")
        }
        // Check that there are some datamodels, and get them
        val theseModels:Vector[DataModel] = cexLibrary.dataModels match {
            case Some(vdm) => vdm
            case _ => throw new ScsException(s"No datamodels in library.")
        }

        // Check for implementation 
        val implementations:Vector[DataModel] = theseModels.filter(_.model == binaryImageModel)
        if (implementations.size < 1){
          logger.info(s"Model ${binaryImageModel} is not represented in this library.")
          throw new ScsException(s"Model ${binaryImageModel} is not represented in this library.")
        }

        // Next, get the collections that implement it
        val colls = implementations.map(i => i.collection)
        // Find which collections connect the DataModel to the requested objectUrn, if any
        val collectionsImplementing:Vector[Cite2Urn] = colls.filter(c => { 
           collectionRepository.get.urnMatch(
            propertyUrnFromPropertyName(c,"collection"),
            urn
          ).size > 0 })


        // Find out which Objects in (each of those/that) Collection implent(s) the collection of the requested URN
        val objectsImplementing:Vector[Cite2Urn] = collectionsImplementing.map( c => {
            val propUrn:Cite2Urn = propertyUrnFromPropertyName(c,"collection")
            val objMatchVec:Vector[CiteObject] = collectionRepository.get.urnMatch(propUrn,urn.dropSelector)
            val objMatchUrns:Vector[Cite2Urn] = objMatchVec.map(_.urn)
            objMatchUrns
          }).flatten

        if (objectsImplementing.size < 1){
          logger.info(s"No object in ${binaryImageModel} implements objects in ${urn.dropSelector}.")
          throw new ScsException(s"No object in ${binaryImageModel} implements objects in ${urn.dropSelector}.")
        }

        val thisImplementation:CiteObject =  implementedByProtocol(objectsImplementing,iiifApiProtocolString) match {
            case Some(o) => o 
            case _ => {
              logger.info(s"This object (${urn}) is not implemented by the ${iiifApiProtocolString}.")
              throw new ScsException(s"This object (${urn}) is not implemented by the ${iiifApiProtocolString}.")
            }
        }


        // Still here? Now let's get some data

        val reply:Map[String,String] = pathAndUrl(urn,thisImplementation)
        reply
      } catch {
        case e: Exception => {
          throw new IOException(e) // was IOException
        }
      }
    }

  // Probably should be in CiteObj library?
  // Given a collection URN and a property name, construct a property URN
  def propertyUrnFromPropertyName(urn:Cite2Urn, propName:String):Cite2Urn = {
    val returnUrn:Cite2Urn = {
        val collUrn:Cite2Urn = {
            urn.propertyOption match {
            case Some(po) => {
              urn.dropProperty.dropSelector
            }
            case None => {
              urn.dropSelector
            }
          }
        }
        val collUrnString:String = collUrn.toString.dropRight(1) // remove colon
        urn.objectComponentOption match {
        case Some(oc) => {
          Cite2Urn(s"${collUrnString}.${propName}:${oc}")
        }
        case None => {
          Cite2Urn(s"${collUrnString}.${propName}:")
        }
      }
    }
    //println(s"returnUrn: ${returnUrn}")
    returnUrn
  } 

  /*
  Given a CITE URN to an object, and a protocol string, report whether that object is implemented by the given protocol
  */
  def implementedByProtocol(urnV:Vector[Cite2Urn], protocol:String):Option[CiteObject] = {
    try {
      urnV.size match {
        case s if (s > 0) => {
          val implementedUrns:Vector[Cite2Urn] = urnV.filter(u => {
            val oneObject:CiteObject = collectionRepository.get.citableObjects.filter(_.urn == u)(0)
            val propId:Cite2Urn = propertyUrnFromPropertyName(u, protocolPropertyName)
            oneObject.valueEquals(propId,protocol)
          })
          implementedUrns.size match {
            case s if (s > 0) => Some(collectionRepository.get.citableObject(implementedUrns(0)))
            case _ => None
          }
        }
        case _ => None
      }
    } catch {
      case e: Exception => {
        None
      }   
    }
  }

  def pathAndUrl(urn:Cite2Urn, obj:CiteObject):Map[String,String] = {

    val pathUrn:Cite2Urn = propertyUrnFromPropertyName(obj.urn, "path")
    val path:String = obj.propertyValue(pathUrn).toString
    val urlUrn:Cite2Urn = propertyUrnFromPropertyName(obj.urn, "url")
    val url:String = obj.propertyValue(urlUrn).toString
    val pathMap:Map[String,String] = Map("serverPath" -> path, "baseUrl" -> url)
    pathMap
  }
 

}
