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
import edu.holycross.shot.citerelation._

case class CiteTripleJson(urn1:String, relation:String, urn2:String)
case class VectorOfCiteTriplesJson(citeTriples:Vector[CiteTripleJson])

  trait RelationsService extends Protocols {
    implicit val system: ActorSystem
    implicit def executor: ExecutionContextExecutor
    implicit val materializer: Materializer
    implicit val cexLibrary:CiteLibrary
    implicit val collectionRepository:Option[CiteCollectionRepository]
    implicit val textRepository:Option[TextRepository]
    implicit def propertyUrnFromPropertyName(urn:Cite2Urn, propName:String):Cite2Urn

    def config: Config
    val logger: LoggingAdapter

    val commentaryVerb:Cite2Urn = Cite2Urn("urn:cite2:cite:verbs.v1:commentsOn")
    val commentaryModel:Cite2Urn = Cite2Urn("urn:cite2:cite:datamodels.v1:commentarymodel")
    lazy val deluxeRelationSet:Option[CiteRelationSet] = {
      cexLibrary.relationSet match {
         case Some(rs) => {
            val u2RangeRelations:Vector[CiteTriple] = {
              rs.relations.filter( ct => {
                  ct.urn2 match {
                    case CtsUrn(_) => {
                      val u2:CtsUrn = ct.urn2.asInstanceOf[CtsUrn]
                      if (u2.isRange) {
                        true
                      } else {
                        false
                      }
                    }
                    case _ => false
                  } 
              }).toVector
            }
            val u2ExpandedUrns:Vector[CiteTriple] = {
              cexLibrary.textRepository match {
                case None => Vector()
                case Some(tr) => {
                  u2RangeRelations.map(rr => {
                    val u2:CtsUrn = rr.urn2.asInstanceOf[CtsUrn]
                    val expandedU2:Vector[CtsUrn] = tr.corpus.validReff(u2)
                    val eU2 = expandedU2.map( eu2 => {
                        val tempRelation:Cite2Urn = rr.relation
                        val u1:Urn = rr.urn1
                        val tempCiteTriple:CiteTriple = CiteTriple(u1, tempRelation, eu2) 
                        tempCiteTriple
                    }).toVector
                    eU2
                  }).flatten 
                }
              } 
            }
            val u1RangeRelations:Vector[CiteTriple] = {
              rs.relations.filter( ct => {
                  ct.urn1 match {
                    case CtsUrn(_) => {
                      val u1:CtsUrn = ct.urn1.asInstanceOf[CtsUrn]
                      if (u1.isRange) {
                        true
                      } else {
                        false
                      }
                    }
                    case _ => false
                  } 
              }).toVector
            }
            val u1ExpandedUrns:Vector[CiteTriple] = {
              cexLibrary.textRepository match {
                case None => Vector()
                case Some(tr) => {
                  u1RangeRelations.map(rr => {
                    val u1:CtsUrn = rr.urn1.asInstanceOf[CtsUrn]
                    val expandedU1:Vector[CtsUrn] = tr.corpus.validReff(u1)
                    val eU1 = expandedU1.map( eu1 => {
                        val tempRelation:Cite2Urn = rr.relation
                        val u2:Urn = rr.urn2
                        val tempCiteTriple:CiteTriple = CiteTriple(eu1, tempRelation, u1) 
                        tempCiteTriple
                    }).toVector
                    eU1
                  }).flatten 
                }
              } 
            }
            val newTriplesVec:Vector[CiteTriple] = u2ExpandedUrns ++ u1ExpandedUrns
            val newTriplesSet:Set[CiteTriple] = newTriplesVec.toSet
            logger.info(s"original triples: ${rs.relations.size}")
            logger.info(s"new triples: ${newTriplesSet.size}")
            val allTriplesSet:Set[CiteTriple] = rs.relations ++ newTriplesSet
            val newCiteRelationSet:CiteRelationSet = CiteRelationSet(allTriplesSet)
            Some(newCiteRelationSet)
         }
         case None => None
      }
    }

    def hasRelations:Boolean = {
       cexLibrary.relationSet match {
          case Some(rs) => true
          case None => false
       }
    }

    def hasCommentaryModel:Boolean = {
      if (hasRelations) {
        cexLibrary.dataModels match {
          case Some(dm) => {
             (dm.filter(_.model == commentaryModel).size > 0) 
          }
          case None => false
        }
      } else { false } // no point talking about a data model if there are no relations
    }   

    def getRelations(urnVec:Vector[Urn], filterVerb:Option[Cite2Urn]):Option[CiteRelationSet] = {
      if (hasRelations) {
        val returnSet:Set[CiteTriple] = {
          filterVerb match {
            case Some(v) => {
              val relationsVec:Vector[CiteTriple] = {
                urnVec.map( u => {
                  deluxeRelationSet.get.relations.filter( rt => { (rt ~~ u) })
                }).flatten
              }
              relationsVec.filter(_.relation == v).toSet
            }
            case None => {
              val relationsVec:Vector[CiteTriple] = {
                urnVec.map( u => {
                  deluxeRelationSet.get.relations.filter( rt => { (rt ~~ u) })
                }).flatten
              }
              relationsVec.toSet
            }
          } 
        }
        returnSet.size match {
          case n if (n > 0) => Some(CiteRelationSet(returnSet))
          case _ => None
        }
      } else { None }
    }

    def getRelations(urn:Urn, filterVerb:Option[Cite2Urn]):Option[CiteRelationSet] = {
      urn match {
        case CtsUrn(_) => {
          val cu:CtsUrn = urn.asInstanceOf[CtsUrn]
          if (cu.isRange){
            cexLibrary.textRepository match {
              case Some(tr) => {
                val expandedUrn:Vector[CtsUrn] = tr.corpus.validReff(cu)
                expandedUrn.size match {
                  case n if (n == 0) => {
                    getSimpleRelations(urn, filterVerb)
                  }
                  case _ => {
                    // let's not forget the original query!
                    val lookupVector:Vector[CtsUrn] = expandedUrn ++ Vector(cu)
                    getRelations(lookupVector, filterVerb)
                   } 
                }
              }
              case None => getSimpleRelations(urn, filterVerb)
            } 
          } else {
            getSimpleRelations(urn, filterVerb)
          }
        }
        case _ => getSimpleRelations(urn, filterVerb)
      }
    }
 
    def getSimpleRelations(urn:Urn, filterVerb:Option[Cite2Urn]):Option[CiteRelationSet] = {
      if (hasRelations) {
        val returnSet:Set[CiteTriple] = {
          filterVerb match {
            case Some(v) => {
              deluxeRelationSet.get.relations.filter( rt =>{
                (rt ~~ urn)
              }).filter(_.relation == v)
            } 
            case None => {
              deluxeRelationSet.get.relations.filter( rt =>{
                (rt ~~ urn)
              })
            }
          } 
        }
        returnSet.size match {
          case n if (n > 0) => Some(CiteRelationSet(returnSet))
          case _ => None
        }
      } else { None }
    }

    def getVerbs:Option[Vector[Cite2Urn]] = {
      if (hasRelations) {
        val verbSet:Set[Cite2Urn] = deluxeRelationSet.get.verbs
        Some(verbSet.toVector)
      } else { None }
    }

    def relationsReturnVerbMap: Future[Either[String,ObjectLabelMapJson]] = {
      try {
          val verbs:Option[Vector[Cite2Urn]] = getVerbs
          val m:Map[String,String] = {
            verbs match {
              case Some(vl) => {
                collectionRepository match {
                  case Some(cr) => {
                    Map(vl.map{ v => 
                      v.toString -> {
                        if (cr.citableObjects.contains(v)){
                          cr.citableObject(v).label 
                        } else {
                          v.objectComponent
                        }
                      }
                    }: _* )
                  }
                  case None => {
                    Map(vl.map{ v => v.toString -> v.objectComponent }: _* ) 
                  }
                }
              }
              case None => Map.empty
            }
            // Map(collectionRepository.get.citableObjects.map{ a => a.urn.toString -> a.label }: _*)
          } 
          val olm:ObjectLabelMapJson = ObjectLabelMapJson(m)
          Unmarshal(olm).to[ObjectLabelMapJson].map(Right(_))
      } catch {
        case e: Exception => {
          Future.successful(Left(s"${new IOException(e)}"))
        }
      }
    }

    def makeCiteTripleJson(ct:CiteTriple):CiteTripleJson = {
      CiteTripleJson(ct.urn1.toString, ct.relation.toString, ct.urn2.toString)
    }


    def makeVectorOfCiteTriplesJson(vct:Vector[CiteTriple]):VectorOfCiteTriplesJson = {
      VectorOfCiteTriplesJson(vct.map( ct => makeCiteTripleJson(ct) )) 
    }
  
    def relationsReturnVerbs:Future[Either[String,VectorOfCite2UrnsJson]] = {
      try {
        val verbSet:Option[Vector[Cite2Urn]] = getVerbs
        val verbJson:VectorOfCite2UrnsJson = {
          verbSet match {
            case Some(vs) => {
              VectorOfCite2UrnsJson(vs.map( u => Cite2UrnString(u.toString)))
            }
            case None => VectorOfCite2UrnsJson(Vector())
          }
        }
        Unmarshal(verbJson).to[VectorOfCite2UrnsJson].map(Right(_))
      } catch {
        case e: Exception => {
          Future.successful(Left(s"${new IOException(e)}"))
        }
      }
    }

    def relationsReturnRelations(urnString:String, filter:Option[String]): Future[Either[String,VectorOfCiteTriplesJson]] = {
      try {
        val urn:Urn = {
          urnString.take(8) match {
            case "urn:cts:" => CtsUrn(urnString)
            case _ => Cite2Urn(urnString)
          }
        }
        val filterUrnOption:Option[Cite2Urn] = {
          filter match {
            case Some(f) => Some(Cite2Urn(f))
            case None => None
          }
        }
        val relationSet:Option[CiteRelationSet] = getRelations(urn, filterUrnOption)
        val vecCiteTripleJson:Vector[CiteTripleJson] = {
          relationSet match {
            case Some(rs) => rs.relations.map(t => makeCiteTripleJson(CiteTriple(t.urn1, t.relation, t.urn2))).toVector
            case None => Vector()
          }
        }
        val returnVal:VectorOfCiteTriplesJson = VectorOfCiteTriplesJson(vecCiteTripleJson)
        Unmarshal(returnVal).to[VectorOfCiteTriplesJson].map(Right(_))
      } catch {
        case e: Exception => {
          Future.successful(Left(s"${new IOException(e)}"))
        }
      }
    }
 

    def getCommentaryForText(uv:Vector[CtsUrn]): VectorOfCiteTriplesJson = {
      try {
        val relationSet:Option[CiteRelationSet] = {
          getRelations(uv, Some(commentaryVerb) )
        }
        val vecCiteTripleJson:Vector[CiteTripleJson] = {
          relationSet match {
            case Some(rs) => rs.relations.map(t => makeCiteTripleJson(CiteTriple(t.urn1, t.relation, t.urn2))).toVector
            case None => Vector()
          }
        }
        val returnVal:VectorOfCiteTriplesJson = VectorOfCiteTriplesJson(vecCiteTripleJson)
        returnVal

      } catch {
        case e: Exception => {
          VectorOfCiteTriplesJson(Vector())
        }
      }
    }

}
