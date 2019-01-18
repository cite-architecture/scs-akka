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

case class DseRecordJson(label:String, passage:String, imageroi:String, surface:String, citeObject:CiteObjectJson)
case class VectorOfDseRecordsJson(dseRecords:Vector[DseRecordJson])

  trait DseService extends Protocols {
    implicit val system: ActorSystem
    implicit def executor: ExecutionContextExecutor
    implicit val materializer: Materializer
    implicit val cexLibrary:CiteLibrary
    implicit val collectionRepository:Option[CiteCollectionRepository]
    implicit val textRepository:Option[TextRepository]
    implicit def makeCiteObjectJson(objectFound:CiteObject):CiteObjectJson
    implicit def propertyUrnFromPropertyName(urn:Cite2Urn, propName:String):Cite2Urn

    def config: Config
    val logger: LoggingAdapter


    val dseModel:Cite2Urn = Cite2Urn("urn:cite2:cite:datamodels.v1:dse")
    val passagePropString:String = "passage"
    val imagePropString:String = "imageroi"
    val surfacePropString:String = "surface"

    def hasDseModel(dseModel:Cite2Urn):Boolean = {
      cexLibrary.dataModels match {
        case Some(dms) => {
           ( dms.filter(_.model == dseModel).size > 0 )
        }
        case None => false
      }
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

    def dseRecordsForImage(imgUrnString:String):Future[Either[String,VectorOfDseRecordsJson]] = {
      try {
        val u:Cite2Urn = Cite2Urn(imgUrnString).dropExtensions
        val vodr:Vector[DseRecordJson] = { 
          if (hasDseModel(dseModel)) {
            val dseCollections:Vector[Cite2Urn] = cexLibrary.collectionsForModel(dseModel)
            dseCollections.size match {
              case n if (n > 0) => {
                val recVec:Vector[DseRecord] = dseCollections.map(dc => {

                  // Set up DSE Configuration
                  val repo:CiteCollectionRepository = collectionRepository.get
                  val lbl:String = collectionRepository.get.collectionDefinition(dc).get.collectionLabel
                  val psgPropUrn:Cite2Urn = propertyUrnFromPropertyName(dc,passagePropString)
                  val imgPropUrn:Cite2Urn = propertyUrnFromPropertyName(dc,imagePropString) 
                  val surPropUrn:Cite2Urn =propertyUrnFromPropertyName(dc,surfacePropString) 
                  val dseExt:DseConfiguration = DseConfiguration(
                    repo,
                    lbl,
                    psgPropUrn,
                    imgPropUrn,
                    surPropUrn
                  )

                  // Get Records
                  val recVec:Vector[DseRecord] = dseExt.recordsForImage(u)
                  recVec
                }).flatten
                val jsonRecVec:Vector[DseRecordJson] = {
                  recVec.map( rv => {
                      makeDseRecordJson(rv)
                  }).toVector
                }
                jsonRecVec
              }
              case _ => Vector()
            }

          } else {
            Vector()
          }
        }
        val returnVal:VectorOfDseRecordsJson = VectorOfDseRecordsJson(vodr)
        Future.successful(Right(returnVal))
      } catch {
        case e: Exception => {
          Future.successful(Left(s"${new IOException(e)}")) // was IOException
        }
      }
    }

 def dseRecordsForSurface(surfUrnString:String):Future[Either[String,VectorOfDseRecordsJson]] = {
     try {
        val u:Cite2Urn = Cite2Urn(surfUrnString).dropExtensions
        val vodr:Vector[DseRecordJson] = { 
          if (hasDseModel(dseModel)) {
            val dseCollections:Vector[Cite2Urn] = cexLibrary.collectionsForModel(dseModel)
            dseCollections.size match {
              case n if (n > 0) => {
                val recVec:Vector[DseRecord] = dseCollections.map(dc => {

                  // Set up DSE Configuration
                  val repo:CiteCollectionRepository = collectionRepository.get
                  val lbl:String = collectionRepository.get.collectionDefinition(dc).get.collectionLabel
                  val psgPropUrn:Cite2Urn = propertyUrnFromPropertyName(dc,passagePropString)
                  val imgPropUrn:Cite2Urn = propertyUrnFromPropertyName(dc,imagePropString) 
                  val surPropUrn:Cite2Urn =propertyUrnFromPropertyName(dc,surfacePropString) 
                  val dseExt:DseConfiguration = DseConfiguration(
                    repo,
                    lbl,
                    psgPropUrn,
                    imgPropUrn,
                    surPropUrn
                  )

                  // Get Records
                  val recVec:Vector[DseRecord] = dseExt.recordsForSurface(u)
                  recVec
                }).flatten
                val jsonRecVec:Vector[DseRecordJson] = {
                  recVec.map( rv => {
                      makeDseRecordJson(rv)
                  }).toVector
                }
                jsonRecVec
              }
              case _ => Vector()
            }

          } else {
            Vector()
          }
        }
        val returnVal:VectorOfDseRecordsJson = VectorOfDseRecordsJson(vodr)
        Future.successful(Right(returnVal))
      } catch {
        case e: Exception => {
          Future.successful(Left(s"${new IOException(e)}")) // was IOException
        }
      }
    }

    def dseRecordsForText(textUrnString:String):Future[Either[String,VectorOfDseRecordsJson]] = {
     try {
        val urn:CtsUrn = CtsUrn(textUrnString)
        val vodr:Vector[DseRecordJson] = { 
          if (hasDseModel(dseModel)) {
            val dseCollections:Vector[Cite2Urn] = cexLibrary.collectionsForModel(dseModel)
            dseCollections.size match {
              case n if (n > 0) => {
                // In SCS-Akka, we don't deliver DSEs for texts we don't have
                textRepository match {
                  case Some(tr) => {
                    //val vurn:Vector[CtsUrn] = (tr.corpus >= urn).nodes.map(_.urn)
                    val vurn:Vector[CtsUrn] = tr.corpus.validReff(urn)
                    val recVec:Vector[DseRecord] = dseCollections.map(dc => {

                     // Set up DSE Configuration
                    val repo:CiteCollectionRepository = collectionRepository.get
                    val lbl:String = collectionRepository.get.collectionDefinition(dc).get.collectionLabel
                    val psgPropUrn:Cite2Urn = propertyUrnFromPropertyName(dc,passagePropString)
                    val imgPropUrn:Cite2Urn = propertyUrnFromPropertyName(dc,imagePropString) 
                    val surPropUrn:Cite2Urn =propertyUrnFromPropertyName(dc,surfacePropString) 
                    val dseExt:DseConfiguration = DseConfiguration(
                      repo,
                      lbl,
                      psgPropUrn,
                      imgPropUrn,
                      surPropUrn
                    )

                    // Get Records
                    val textRecVec:Vector[DseRecord] = dseExt.recordsForTextVector(vurn)
                    textRecVec


                    }).flatten
                    val jsonRecVec:Vector[DseRecordJson] = {
                      recVec.map( rv => {
                          makeDseRecordJson(rv)
                      }).toVector
                    }
                    jsonRecVec
                  }
                  case None => Vector()
                }
              }
              case _ => Vector()
            }
          } else {
            Vector()
          }
        }
        val returnVal:VectorOfDseRecordsJson = VectorOfDseRecordsJson(vodr)
        Future.successful(Right(returnVal))
      } catch {
        case e: Exception => {
          Future.successful(Left(s"${new IOException(e)}")) // was IOException
        }
      }
    }

def dseRecordsComprehensive(uv:Vector[Urn]):VectorOfDseRecordsJson = {
  try {
    val ctsUs:Vector[CtsUrn] = {
      uv.filter(u =>{
          u match {
            case CtsUrn(_) => true
            case _ => false
          } 
      }).map(u => u.asInstanceOf[CtsUrn]) 
    }
    // expand CtsUrns
    val expandedCtsUs:Vector[CtsUrn] = {
      textRepository match { 
        case Some(tr) => {
          val firstCut:Vector[CtsUrn] = (for (u <- ctsUs) yield {
            tr.corpus.validReff(u)
          }).flatten
          val originalVec:Vector[CtsUrn] = {
            ctsUs.map (_.asInstanceOf[CtsUrn])
          }
          (firstCut ++ originalVec).distinct
        }
        case None => ctsUs
      }
    }
    val cite2Us:Vector[Cite2Urn] = {
      uv.filter(u =>{
          u match {
            case Cite2Urn(_) => true
            case _ => false
          } 
      }).map(u => u.asInstanceOf[Cite2Urn]) 
    }
    val allUrns:Vector[Urn] = expandedCtsUs ++ cite2Us
    val dseRecs:Vector[DseRecord] = dseRecordsForUrnVector(allUrns).distinct
    val dseRecsJson:Vector[DseRecordJson] = {
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
def dseRecordsForUrnVector(uv:Vector[Urn]):Vector[DseRecord] = {
  if (hasDseModel(dseModel)) {
    val dseCollections:Vector[Cite2Urn] = cexLibrary.collectionsForModel(dseModel)
    dseCollections.size match {
      case n if (n > 0) => {
        // We'll do this for every DSE Collection…
        val recVec:Vector[DseRecord] = dseCollections.map(dc => {
          // Set up DSE Configuration
          val repo:CiteCollectionRepository = collectionRepository.get
          val lbl:String = collectionRepository.get.collectionDefinition(dc).get.collectionLabel
          val psgPropUrn:Cite2Urn = propertyUrnFromPropertyName(dc,passagePropString)
          val imgPropUrn:Cite2Urn = propertyUrnFromPropertyName(dc,imagePropString) 
          val surPropUrn:Cite2Urn =propertyUrnFromPropertyName(dc,surfacePropString) 
          val dseExt:DseConfiguration = DseConfiguration(
            repo,
            lbl,
            psgPropUrn,
            imgPropUrn,
            surPropUrn
          ) 
          // now cycle through each Urn
          val oneVector = {
            uv.map(u => {
              u match {
                case CtsUrn(_) => {
                  val textRecVec:Vector[DseRecord] = dseExt.recordsForTextVector(Vector(u.asInstanceOf[CtsUrn]))
                  textRecVec
                }
                case Cite2Urn(_) => {
                  val surfRecVec:Vector[DseRecord] = dseExt.recordsForSurface(u.asInstanceOf[Cite2Urn])
                  val imgRecVec:Vector[DseRecord] = dseExt.recordsForImage(u.asInstanceOf[Cite2Urn])
                  val combinedVec:Vector[DseRecord] = surfRecVec ++ imgRecVec
                  combinedVec
                } 
              } 
            }).toVector.flatten
          }
          oneVector

        }).toVector.flatten.distinct
        recVec
      }
      case _ => {
        Vector[DseRecord]()
      }
    }
  }  else {
    Vector[DseRecord]()
  }
}

def makeDseRecordJson(dr:DseRecord):DseRecordJson = {
    val label:String = dr.label 
    val passage:String = dr.passage.toString
    val imageroi:String = dr.imageroi.toString
    val surface:String = dr.surface.toString
    val citeObject:CiteObjectJson = makeCiteObjectJson(dr.citeObject)
    DseRecordJson(label, passage, imageroi, surface, citeObject)
}
   
 

  

 
 

}
