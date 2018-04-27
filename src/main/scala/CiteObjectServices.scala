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

case class CiteObjectJson(citeObject:Map[String,String],citePropertyValues:
    Vector[Map[String,String]])
case class VectorOfCiteObjectsJson(citeObjects:Vector[CiteObjectJson])
case class CitePropertyDefJson(citePropertyDef:Map[String,String])
case class CiteCollectionInfoJson(citeCollectionInfo:Map[String,String])
case class CiteCollectionPropertyDefsJson(citeCollectionPropertyDefs:Vector[CitePropertyDefJson])
case class CiteCollectionDefJson(citeCollectionDef:CiteCollectionInfoJson, citeProperties:CiteCollectionPropertyDefsJson)
case class VectorOfCiteCollectionDefsJson(citeCollectionDefs:Vector[CiteCollectionDefJson])
// Data Model
case class DataModelDefJson(dataModel:Map[String,String])
case class ObjectLabelMapJson(labelMap:Map[String,String])
case class VectorOfDataModelsDefJson(dataModels:Vector[DataModelDefJson])


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

  def fetchPrevCite2Urn(urnString: String): Future[Either[String,Cite2UrnString]] = {
    try {
      val urn:Cite2Urn = {
        val u = Cite2Urn(urnString)
        if (u.isRange) {
          u.rangeBeginUrn 
        } else {
          u
        }
      }
      val thisObj:CiteObject = collectionRepository.get.citableObject(urn)
      val urnReplyString:String = {
        collectionRepository.get.prev(thisObj) match {
          case Some(obj:CiteObject) => obj.urn.toString
          case None => ""
        }
      }
      val urnReply:Cite2UrnString = Cite2UrnString(urnReplyString)
      Unmarshal(urnReply).to[Cite2UrnString].map(Right(_))
    } catch {
      case e: Exception => {
        Future.successful(Left(s"${new IOException(e)}"))
      }
    }
  }

  def fetchNextCite2Urn(urnString: String): Future[Either[String,Cite2UrnString]] = {
    try {
      val urn:Cite2Urn = {
        val u = Cite2Urn(urnString)
        if (u.isRange) {
          u.rangeEndUrn 
        } else {
          u
        }
      }
      val thisObj:CiteObject = collectionRepository.get.citableObject(urn)
      val urnReplyString:String = {
        collectionRepository.get.next(thisObj) match {
          case Some(obj:CiteObject) => obj.urn.toString
          case None => ""
        }
      }
      val urnReply:Cite2UrnString = Cite2UrnString(urnReplyString)
      Unmarshal(urnReply).to[Cite2UrnString].map(Right(_))
    } catch {
      case e: Exception => {
        Future.successful(Left(s"${new IOException(e)}"))
      }
    }
  }

  def fetchObjectsMapJson: Future[Either[String,ObjectLabelMapJson]] = {
    try {
        val m:Map[String,String] = {
          Map(collectionRepository.get.citableObjects.map{ a => a.urn.toString -> a.label }: _*)
        } 
        val olm:ObjectLabelMapJson = ObjectLabelMapJson(m)
        Unmarshal(olm).to[ObjectLabelMapJson].map(Right(_))
    } catch {
      case e: Exception => {
        Future.successful(Left(s"${new IOException(e)}"))
      }
    }
  }

  def fetchCiteCollectionDef(urn: Cite2Urn) : Option[CiteCollectionDefJson] = {
    try {
      val ccd:Option[CiteCollectionDef] = collectionRepository.get.collectionDefinition(urn.dropSelector)
      val urnReply:Option[CiteCollectionDefJson] = ccd match {
        case Some(d) => {
          val dUrn:String = d.urn.toString
          val dLabel:String = d.collectionLabel
          val dLicense:String = d.license
          val dOrdering:String = d.orderingProperty match {
            case Some(op) => op.toString
            case None => ""
          }
          val dLabelling:String = d.labellingProperty match {
              case Some(lp) => lp.toString
              case None => ""
          }
          val dPropertyDefs:CiteCollectionPropertyDefsJson = {
            CiteCollectionPropertyDefsJson(
              d.propertyDefs.map( cpd => {
                val tempPd = fetchPropertyDef(cpd)
                tempPd
              })
            )
          }
          Some(CiteCollectionDefJson(
              CiteCollectionInfoJson(Map("urn" -> dUrn,
              "collectionLabel" -> dLabel,
              "license" -> dLicense,
              "labellingProperty" -> dLabelling,
              "orderingProperty" -> dOrdering)),
              dPropertyDefs
          ))
        }
        case None => None
        }
      urnReply
    } catch {
      case e: Exception => {
        throw new ScsException(s"Exception creating collection definition for ${urn}.")
      }
    }
  }

   def fetchCiteCollectionDefJson(urnString: String): Future[Either[String, CiteCollectionDefJson]] = {
    try {
      val urn:Cite2Urn = Cite2Urn(urnString)
      val ccdj:Option[CiteCollectionDefJson] = fetchCiteCollectionDef(urn)
      ccdj match {
        case Some(cc) => Unmarshal(cc).to[CiteCollectionDefJson].map(Right(_))
        case _ => Future.successful(Left(s"No collection found"))
      }
    } catch {
      case e: Exception => {
        Future.successful(Left(s"${new IOException(e)}"))
      }
    }
  }

  def fetchVectorOfCiteCollectionDefsJson: Future[Either[String, VectorOfCiteCollectionDefsJson]] ={
    try {
      val tempVec:Vector[CiteCollectionDefJson] = (
        collectionRepository.get.catalog.collections.map( cc => {
         val ccd:Option[CiteCollectionDefJson] = fetchCiteCollectionDef(cc.urn)
         ccd match {
            case Some(c:CiteCollectionDefJson) => c 
            case None => null
         }
       })).toVector
      val collVec:VectorOfCiteCollectionDefsJson = VectorOfCiteCollectionDefsJson(tempVec)
      Unmarshal(collVec).to[VectorOfCiteCollectionDefsJson].map(Right(_))
    } catch {
      case e: Exception => {
        Future.successful(Left(s"${new IOException(e)}"))
      }
    }
  }


/*
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
              case Some(lp) => Some(lp.toString)
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
    */


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

    def fetchPagedCiteObjectJson(urnString: String, offset:Int, limit:Int): Future[Either[String, VectorOfCiteObjectsJson]] = {
      try {
        val trueOffset = offset 
        val urn:Cite2Urn = Cite2Urn(urnString)
        val unpagedReply:VectorOfCiteObjectsJson = fetchCiteObjects(urn)
        val pagedReply:VectorOfCiteObjectsJson = VectorOfCiteObjectsJson(unpagedReply.citeObjects.slice(trueOffset,(trueOffset + limit)) )
        Unmarshal(pagedReply).to[VectorOfCiteObjectsJson].map(Right(_))
      } catch {
        case e: Exception => {
          Future.successful(Left(s"${new IOException(e)}"))
        }
      }
    }

    def fetchCiteObjects(urn: Cite2Urn):VectorOfCiteObjectsJson = {
      try {
        //logger.info(s"++++ fetchCiteObjects for ${urn}")
        val vectorReply:Vector[CiteObject] = collectionRepository.get ~~ urn
        //logger.info(s"++++ got ${vectorReply.size}")
        VectorOfCiteObjectsJson(vectorReply.map( v => {
          val c:CiteObjectJson = makeCiteObjectJson(v) 
          c
        }))
      } catch {
        case e: Exception => {
          throw new ScsException(s"""Failed to make vector of objects for ${urn}.""")
        }
      }
    }

def doUrnMatch(collectionUrnStr:Option[String], urnToMatchStr:String, parameterUrnStr:Option[String] ): Future[Either[String, VectorOfCiteObjectsJson]] = {
  try {
    val urnToMatch:Urn = {
      if (urnToMatchStr.startsWith("urn:cts:")) { CtsUrn(urnToMatchStr) } 
      else { Cite2Urn(urnToMatchStr) }
    }
    val co:Vector[CiteObject] = collectionUrnStr match {
      case Some(u) => {
        val collectionUrn:Cite2Urn = Cite2Urn(u).dropSelector
        val thisVec:Vector[CiteObject] = collectionRepository.get.citableObjects.filter(_.urn.dropSelector == collectionUrn)
        thisVec
      }
      case None => collectionRepository.get.citableObjects
    }
    val findResults:Vector[CiteObject] = parameterUrnStr match {
      case Some(pus) => co.filter(_.urnMatch(Cite2Urn(pus), urnToMatch))
      case None => co.filter(_.urnMatch(urnToMatch))
    }
    val objectVector:VectorOfCiteObjectsJson = VectorOfCiteObjectsJson(findResults.map(o => makeCiteObjectJson(o)))
    Unmarshal(objectVector).to[VectorOfCiteObjectsJson].map(Right(_))
  } catch {
    case e: Exception => {
      Future.successful(Left(s"${new IOException(e)}"))
    }
  }
}

def doRegexMatch(collectionUrnStr:Option[String], regexToMatchStr:String, parameterUrnStr:Option[String] ): Future[Either[String, VectorOfCiteObjectsJson]] = {
  try {
    val co:Vector[CiteObject] = collectionUrnStr match {
      case Some(u) => {
        val collectionUrn:Cite2Urn = Cite2Urn(u).dropSelector
        val thisVec:Vector[CiteObject] = collectionRepository.get.citableObjects.filter(_.urn.dropSelector == collectionUrn)
        thisVec
      }
      case None => collectionRepository.get.citableObjects
    }
    val findResults:Vector[CiteObject] = parameterUrnStr match {
      case Some(pus) => co.filter(_.regexMatch(Cite2Urn(pus), regexToMatchStr))
      case None => co.filter(_.regexMatch(regexToMatchStr))
    }
    val objectVector:VectorOfCiteObjectsJson = VectorOfCiteObjectsJson(findResults.map(o => makeCiteObjectJson(o)))
    Unmarshal(objectVector).to[VectorOfCiteObjectsJson].map(Right(_))
  } catch {
    case e: Exception => {
      Future.successful(Left(s"${new IOException(e)}"))
    }
  }
}

def doStringContains(collectionUrnStr:Option[String], stringToMatchStr:String, caseSensitive:Boolean = true): Future[Either[String, VectorOfCiteObjectsJson]] = {
  try {
    val co:Vector[CiteObject] = collectionUrnStr match {
      case Some(u) => {
        val collectionUrn:Cite2Urn = Cite2Urn(u).dropSelector
        val thisVec:Vector[CiteObject] = collectionRepository.get.citableObjects.filter(_.urn.dropSelector == collectionUrn)
        thisVec
      }
      case None => collectionRepository.get.citableObjects
    }
    val findResults:Vector[CiteObject] = co.filter(_.stringContains(stringToMatchStr, caseSensitive))
    val objectVector:VectorOfCiteObjectsJson = VectorOfCiteObjectsJson(findResults.map(o => makeCiteObjectJson(o)))
    Unmarshal(objectVector).to[VectorOfCiteObjectsJson].map(Right(_))
  } catch {
    case e: Exception => {
      Future.successful(Left(s"${new IOException(e)}"))
    }
  }
}

def doNumeric(collectionUrnStr:Option[String], n1:Double, op:String, n2:Option[Double], propertyUrnStr:Option[String]):Future[Either[String,VectorOfCiteObjectsJson]] = {
  try {  
    val co:Vector[CiteObject] = collectionUrnStr match {
      case Some(u) => {
        val collectionUrn:Cite2Urn = Cite2Urn(u).dropSelector
        val thisVec:Vector[CiteObject] = collectionRepository.get.citableObjects.filter(_.urn.dropSelector == collectionUrn)
        thisVec
      }
      case None => collectionRepository.get.citableObjects
    }
    val findResults:Vector[CiteObject] = propertyUrnStr match {
      case Some(purns) => {
        val purn:Cite2Urn = Cite2Urn(purns)
        purn.propertyOption match {
          case Some(po) => {
            op match {
              case "lt" => {
                  co.filter(_.numericLessThan(purn, n1))
              }
              case "lteq" => {
                  co.filter(_.numericLessThanOrEqual(purn, n1))
              }
              case "eq" => {
                  co.filter(_.numericWithin(purn, n1,n1))
              }
              case "gt" => {
                  co.filter(_.numericGreaterThan(purn, n1))
              }
              case "gteq" => {
                  co.filter(_.numericGreaterThanOrEqual(purn, n1))
              }
              case "within" => {
                  n2 match {
                    case Some(d) => {
                      co.filter(_.numericWithin(purn, n1, d))
                    }
                    case _ => throw new ScsException(s"""Two parameters of type Double are required for "within". """)
                  } 
              }
              case _ => throw new ScsException(s"""${op} is not a recognized operator. Should be "lt", "lteq", "eq", "gt", "gteq", or "within". """)
            }    
          }
          case None => throw new ScsException(s"""${purns} lacks a property id.""")
        }
      }
      case None => {
        op match {
          case "lt" => {
              co.filter(_.numericLessThan(n1))
          }
          case "lteq" => {
              co.filter(_.numericLessThanOrEqual(n1))
          }
          case "eq" => {
              co.filter(_.numericWithin(n1,n1))
          }
          case "gt" => {
              co.filter(_.numericGreaterThan(n1))
          }
          case "gteq" => {
              co.filter(_.numericGreaterThanOrEqual(n1))
          }
          case "within" => {
              n2 match {
                case Some(d) => {
                  co.filter(_.numericWithin(n1, d))
                }
                case _ => throw new ScsException(s"""Two parameters of type Double are required for "within". """)
              } 
          }
          case _ => throw new ScsException(s"""${op} is not a recognized operator. Should be "lt", "lteq", "eq", "gt", "gteq", or "within". """)
        }    
      }
    }
    val objectVector:VectorOfCiteObjectsJson = VectorOfCiteObjectsJson(findResults.map(o => makeCiteObjectJson(o)))
    Unmarshal(objectVector).to[VectorOfCiteObjectsJson].map(Right(_))
  } catch {
    case e: Exception => {
      Future.successful(Left(s"${new IOException(e)}"))
    }
  }
}

def doValueEquals(propertyUrnStr:String, valueToMatchStr:String): Future[Either[String, VectorOfCiteObjectsJson]] = {
  try {
    val propertyUrn:Cite2Urn = Cite2Urn(propertyUrnStr) 
    val collectionUrn:Cite2Urn = propertyUrn.dropSelector.dropProperty
    val collectionDef:Option[CiteCollectionDef] = collectionRepository.get.collectionDefinition(collectionUrn)
    if (collectionDef == None){ throw new ScsException(s"${collectionUrn} is not present in this repository.")} 
    val propertyDefVec:Vector[CitePropertyDef] = collectionDef.get.propertyDefs.filter(_.urn == propertyUrn)
    if (propertyDefVec.size < 1) { throw new ScsException(s"${propertyUrn} is not a property of ${collectionUrn}.")}
    val propertyDef:CitePropertyDef = propertyDefVec(0)
    val propertyType:CitePropertyType = propertyDef.propertyType
    val objectMatches:Vector[CiteObject] = propertyType match {
      case CtsUrnType => { 
        val valueToMatch:CtsUrn = CtsUrn(valueToMatchStr)
        collectionRepository.get.valueEquals(propertyUrn, valueToMatch)
      }
      case Cite2UrnType => { 
        val valueToMatch:Cite2Urn = Cite2Urn(valueToMatchStr)
        collectionRepository.get.valueEquals(propertyUrn, valueToMatch)
      }
      case StringType => {  
        val valueToMatch:String = valueToMatchStr
        collectionRepository.get.valueEquals(propertyUrn, valueToMatch)
      }
      case BooleanType => { 
        val valueToMatch:Boolean = (valueToMatchStr == "true")
        collectionRepository.get.valueEquals(propertyUrn, valueToMatch)
      }
      case NumericType => { 
        val valueToMatch:Double = valueToMatchStr.toDouble
        collectionRepository.get.valueEquals(propertyUrn, valueToMatch)
      }        
      case ControlledVocabType => { 
        val valueToMatch:String = valueToMatchStr
        collectionRepository.get.valueEquals(propertyUrn, valueToMatch)
      }
      case _ => { throw new ScsException(s"${propertyUrn} is of an unrecognized type.") }
    }
    val objectMatchesJson:Vector[CiteObjectJson] = objectMatches.map( o => makeCiteObjectJson(o))
    val objectVector:VectorOfCiteObjectsJson = VectorOfCiteObjectsJson(objectMatchesJson)
    Unmarshal(objectVector).to[VectorOfCiteObjectsJson].map(Right(_))
  } catch {
    case e: Exception => {
      Future.successful(Left(s"${new IOException(e)}"))
    }
  }
}

def hasObject(urnString:String):Future[Either[String, Boolean]] = {
  try {
    val urn:Cite2Urn = Cite2Urn(urnString)
    val hasIt:Boolean = citableObjects.get.filter(_.urn == urn).size > 0
    Unmarshal(hasIt).to[Boolean].map(Right(_))
  } catch{
    case e: Exception => {
      Future.successful(Left(s"${new IOException(e)}"))
    }
  }
}

def getPropertyType(purn:Cite2Urn):String = {
  try {
    val propUrn:Cite2Urn = purn.dropSelector
    val collectionUrn = purn.dropSelector.dropProperty
    val cdefs = collectionRepository.get.catalog.collections.filter(_.urn == collectionUrn)
    val cdef:CiteCollectionDef = cdefs(0)
    val ptype:String = cdef.propertyDefs.filter(_.urn == propUrn)(0).propertyType.toString
    ptype
  } catch {
    case e: Exception => {
      throw new ScsException(s"Failed to find type for property ${purn}.")
    }
  }
}

def makeCiteObjectJson(objectFound:CiteObject):CiteObjectJson = {
  try {
    val collectionUrn = objectFound.urn.dropSelector.dropProperty
    val objectMap:Map[String,String] = Map("urn" -> objectFound.urn.toString, "label" -> objectFound.label )
    val objectPropertiesVector:Vector[Map[String,String]] = objectFound.propertyList.map( p => {
      val m = Map(
        "propertyUrn" -> p.urn.toString, 
        "propertyType" -> getPropertyType(p.urn), 
        "propertyValue" -> p.propertyValue.toString,
        "propertyDefLabel" -> p.propertyDef.label,
        "propertyDefVocab" -> p.propertyDef.vocabularyList.mkString(",")
      )
      m 
    })
    val objreply = CiteObjectJson( objectMap, objectPropertiesVector )
    objreply

  } catch {
    case e: Exception => {
      throw new ScsException(s"Failed to make CiteObjectJson.")
    }
  }
}

  def fetchPropertyDefJson(cpd:CitePropertyDef): Future[Either[String,CitePropertyDefJson]] = {
     try {
       val ncopd:CitePropertyDefJson = fetchPropertyDef(cpd:CitePropertyDef)
       Unmarshal(ncopd).to[CitePropertyDefJson].map(Right(_))
      } catch {
      case e: Exception => {
        Future.successful(Left(s"${new IOException(e)}"))
      }
    }
  }

  def fetchPropertyDef(cpd:CitePropertyDef):CitePropertyDefJson = {
    val urn:String = cpd.urn.toString
    val label:String = cpd.label
    val propertyType:String = cpd.propertyType.toString
    val vocabularyList:String = cpd.vocabularyList.mkString(",")
    val ncpd:CitePropertyDefJson = CitePropertyDefJson(Map(
      "urn" -> urn,
      "label" -> label,
      "propertyType" -> propertyType, 
      "vocabularyList" -> vocabularyList
    ))
    ncpd
  }

    def fetchDataModelsJson: Future[Either[String, VectorOfDataModelsDefJson]] = {
     try {
       val ovdm:Option[VectorOfDataModelsDefJson] = fetchDataModels
       ovdm match {
          case Some(vdm) => Unmarshal(vdm).to[VectorOfDataModelsDefJson].map(Right(_))
          case None => Future.successful(Left(s"No Data Models in Collection"))
       }
      } catch {
      case e: Exception => {
        Future.successful(Left(s"${new IOException(e)}"))
      }
    }
  }

    def fetchDataModels:Option[VectorOfDataModelsDefJson] = {
      val dataModelsOpt:Option[Vector[DataModel]] = cexLibrary.dataModels
      val returnVal:Option[VectorOfDataModelsDefJson] = {
        dataModelsOpt match {
          case Some(vdm) => {

             val dms:Vector[DataModelDefJson] = {
              vdm.map(dm => {
                val coll:String = dm.collection.toString
                val desc:String = dm.description
                val lab:String = dm.label
                val mod:String = dm.model.toString
                val dmdjm:DataModelDefJson = DataModelDefJson(Map("collection" -> coll,"description"->desc,"label"->lab,"model"->mod))
                dmdjm
              }).toVector
             } 
             val vdmdj:VectorOfDataModelsDefJson = VectorOfDataModelsDefJson(dms)
             Some(vdmdj)
          }
          case None => None
        } 
      }
      returnVal

    }


}
