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
        val iifService:IIIFApi = IIIFApi(baseUrl, serverPath, width, maxWidth, maxHeight) 
        val responseString:String = iifService.serviceRequest(urn)
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
        val iifService:IIIFApi = IIIFApi(baseUrl, serverPath, width, maxWidth, maxHeight) 
        val responseString:String = iifService.serviceRequest(urn)
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
        // Check for no applicable model
        if (cexLibrary.modelApplies(binaryImageModel,collectionUrn) == false ){
          logger.info(s"Model ${binaryImageModel} does not apply to Collection ${collectionUrn}")
          throw new ScsException(s"Model ${binaryImageModel} does not apply to Collection ${collectionUrn}")
        }
        // Check that the object exists in the collection
        val requestedObjectVector:Vector[CiteObject] = collectionRepository.get ~~ urn
        if (requestedObjectVector.size < 1){
          logger.info(s"Object ${urn} does not exist in Collection ${collectionUrn}")
          throw new ScsException(s"Object ${urn} does not exist in Collection ${collectionUrn}")
        }
        if (requestedObjectVector.size > 1){
          logger.info(s"${urn} seems to identify more than one object in Collection ${collectionUrn}")
          throw new ScsException(s"${urn} seems to identify more than one object in Collection ${collectionUrn}")
        }
        // Check that the binaryImageCollection exists in this data
        val cv:Vector[CiteCollectionDef] = collectionRepository.get.catalog.collections
        val imageCollectionFilter = cv.filter(_.urn == binaryImageCollection).size
        val imageCollectionPresent:Boolean = imageCollectionFilter == 1
        if (imageCollectionPresent != true){
          logger.info(s"The Collection ${binaryImageCollection}, which implements the CiteBinaryImage model, is missing.")
          throw new ScsException(s"The Collection ${binaryImageCollection}, which implements the CiteBinaryImage model, is missing.")
        }


        // If we got here, then we can proceed
        val requestObject:CiteObject = requestedObjectVector(0)

        // Get correct item from BinaryImage Collection
        val bcColl:Vector[CiteObject] = collectionRepository.get.citableObjects.filter(_.urn ~~ binaryImageCollection)
        val bcItems:Vector[CiteObject] = bcColl.filter(_.urnMatch(collectionUrn))
        // Confirm that there is one and only one match
        if (bcItems.size != 1){
          logger.info(s"Ambiguous match for collection ${collectionUrn} in ${binaryImageCollection}.")
          throw new ScsException(s"Ambiguous match for collection ${collectionUrn} in ${binaryImageCollection}.")
        }

        // Still here? Now let's get some data
        val bcItem:CiteObject = bcItems(0)
        val baseUrl:String = bcItem.propertyValue(binaryImageUrlProp).toString
        val serverPath:String = bcItem.propertyValue(binaryImagePathProp).toString

        val reply:Map[String,String] = Map("baseUrl" -> baseUrl, "serverPath" -> serverPath)
        reply
      } catch {
        case e: Exception => {
          throw new IOException(e) // was IOException
        }
      }
    }


}
