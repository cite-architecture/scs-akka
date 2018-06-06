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

case class CtsUrnString(urnString: String)
case class CexLibraries(repos:Vector[CiteLibrary])
case class CorpusJson(citableNodes:Vector[Map[String, String]], dse:VectorOfDseRecordsJson = VectorOfDseRecordsJson(Vector()), commentary:VectorOfCiteTriplesJson = VectorOfCiteTriplesJson(Vector()) )
case class ReffJson(reff:Vector[String])
case class CitableNodeJson(citableNode:Map[String, String])
case class NgramHistoJson(ngramHisto:Vector[Map[String,String]] )
case class CatalogJson(ctsCatalog:Vector[ Map[String,String] ] )
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


  trait Ohco2Service extends Protocols with DseService with RelationsService {
    implicit val system: ActorSystem
    implicit def executor: ExecutionContextExecutor
    implicit val materializer: Materializer
    implicit val cexLibrary:CiteLibrary
    implicit val textRepository:Option[TextRepository]

    def config: Config
    val logger: LoggingAdapter

  def fetchLibraryInfo: Future[Either[String,Map[String,String]]] = {
    try {
      val libraryName:String = cexLibrary.name
      val libraryUrn:Cite2Urn = cexLibrary.urn
      val libraryLicense:String = cexLibrary.license
      val infoMap:Map[String,String] = {
        Map(
          "name" -> libraryName,
          "urn" -> libraryUrn.toString,
          "license" -> libraryLicense
        )
      }
     Unmarshal(infoMap).to[Map[String,String]].map(Right(_))
   } catch {
      case e: Exception => {
        Future.successful(Left(s"${new IOException(e)}"))
      }
    }
  }

  def fetchLabelForUrn(urnString:String): Future[Either[String,String]] = {
    try {
        val u:CtsUrn = CtsUrn(urnString)
        val label:String = textRepository.get.label(u)
        Unmarshal(label).to[String].map(Right(_))
    } catch {
      case e: Exception => {
        Future.successful(Left(s"${new IOException(e)}"))
      }
    }
  }



    def fetchCatalog(urnString:Option[String]):Future[Either[String,CatalogJson]] = {
      try {
        val cc:Vector[CatalogEntry] = urnString match {
          case Some(us) => {
            val urn:CtsUrn = CtsUrn(us).dropPassage
            val preVector:Vector[CatalogEntry] = textRepository.get.catalog.texts
            preVector.filter(_.urn ~~ urn)
          } 
          case None => {
            textRepository.get.catalog.texts
          }
        }
        val v = cc.map(l => 
          Map("urn" -> l.urn.toString, 
           "citationScheme" -> l.citationScheme,
           "lang" -> l.lang,
           "groupName" -> l.groupName,
           "workTitle" -> l.workTitle,
           "versionLabel" -> {
              l.versionLabel match {
                case Some(vl) => vl
                case _ => ""
              }
            },
           "exemplarLabel" -> {
              l.exemplarLabel match {
                case Some(el) => el
                case _ => ""
              }
            }
         ))
        val n:CatalogJson = CatalogJson(v) 
        Unmarshal(n).to[CatalogJson].map(Right(_))
      } catch {
        case e: Exception => {
          Future.successful(Left(s"${new IOException(e)}")) // was IOException
        }
      }
    }

  def fetchReff(urnString: String): Future[Either[String,ReffJson]] = {
   try {
    val urn:CtsUrn = CtsUrn(urnString)

    urn.versionOption match {
      case Some(v) => {
        val c:Corpus = cexLibrary.textRepository.get.corpus >= urn 
        val v:Vector[String] = c.urns.map(u => u.toString)
        val n:ReffJson = ReffJson(v) 
        Unmarshal(n).to[ReffJson].map(Right(_))
      }
      case None => {
        val passageComp:String = urn.passageComponentOption match {
          case Some(s) => s
          case None => ""
          }
          val allUrns:Vector[CtsUrn] = textRepository.get.corpus.citedWorks        
          val realUrns:Vector[CtsUrn] = allUrns.filter(urn.dropPassage == _.dropVersion)
          val corpora:Vector[Corpus] = realUrns.map(ru => {
            //logger.info(ru.toString)
            textRepository.get.corpus >= CtsUrn(s"${ru.dropPassage}${passageComp}")
          })
          val assembledVector:Vector[String] = {
            (for (c <- corpora) yield {
              for (n <- c.urns) yield { n.toString }
            }).flatten
          }
          val n:ReffJson = ReffJson(assembledVector) 
          Unmarshal(n).to[ReffJson].map(Right(_))
        }
      }      

    } catch {
      case e: Exception => {
        Future.successful(Left(s"${new IOException(e)}"))
      }
    }
  }


  def fetchUrnsForNgramJson(urnString: Option[String], ngString: String): Future[Either[String,ReffJson]] = {
   try {
      val nu:Vector[CtsUrn] = urnString match {
        case Some(u) => {
          val urn:CtsUrn = CtsUrn(u)
          val urnv:Vector[CtsUrn] = (textRepository.get.corpus >= urn).urnsForNGram(ngString)
          urnv
        }
        case None => {
          val urnv:Vector[CtsUrn] = textRepository.get.corpus.urnsForNGram(ngString)      
          urnv
        }
      }
      val v:Vector[String] = nu.map(u => u.toString)
      val n:ReffJson = ReffJson(v) 
      Unmarshal(n).to[ReffJson].map(Right(_))
   } catch {
      case e: Exception => {
        Future.successful(Left(s"${new IOException(e)}"))
      }
    }

  }

  def fetchUrnsForNgram(urnString: Option[String], ngString: String):Vector[CtsUrn] = {
   try {
      val nu:Vector[CtsUrn] = urnString match {
        case Some(u) => {
          val urn:CtsUrn = CtsUrn(u)
          val urnv:Vector[CtsUrn] = (textRepository.get.corpus >= urn).urnsForNGram(ngString)
          urnv
        }
        case None => {
          val urnv:Vector[CtsUrn] = textRepository.get.corpus.urnsForNGram(ngString)
          urnv
        }
      }
      nu
   } catch {
      case e: Exception => {
        throw new IOException(e)
      }
    }

  }

  def urnsToKwikCorpus(urnString: Option[String], ngString: String):Future[Either[String,CorpusJson]] = {
    try {
      val vurn:Vector[CtsUrn] = fetchUrnsForNgram(urnString, ngString)
      val tempV1:Vector[CitableNode] = vurn.map(u => {
        textRepository.get.corpus.nodes.filter(_.urn == u)(0)
      }).toVector
      val tempV:Vector[CitableNode] = tempV1.map(n => {
        val newNode:CitableNode = CitableNode(urn = n.urn, text = n.kwic(ngString,30))
        newNode
      }).toVector
      val c:Corpus = Corpus(tempV)
      val v:Vector[Map[String,String]] = c.nodes.map(l => Map("urn" -> l.urn.toString, "text" -> l.text))
      val n:CorpusJson = CorpusJson(v) 
      Unmarshal(n).to[CorpusJson].map(Right(_))
    } catch {
      case e: Exception => {
        Future.successful(Left(s"${new IOException(e)}"))
      }
    }
  }


  /* def fetchOhco2Text(urnString: String): Future[Either[String,CorpusJson]] = {
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
  } */

  def fetchOhco2Text(urnString: String, withDse:Boolean = false, withCommentary:Boolean = false ): Future[Either[String,CorpusJson]] = {
   try {
    val urn:CtsUrn = CtsUrn(urnString)
    //if (urn.passageComponentOption == None){ throw new ScsException("No passage component. This service will not return a whole text.") }

    urn.versionOption match {
      case Some(v) => {
        val c:Corpus = cexLibrary.textRepository.get.corpus >= urn 

        val reff:Vector[CtsUrn] = {
          if (withDse || withCommentary) {
            textRepository.get.corpus.validReff(urn)
          } else {
            Vector()
          }
        }
        // Get DSE records, if any, for the requested text
        val dseRecs:VectorOfDseRecordsJson = {
          if (withDse) dseRecordsComprehensive(reff)
          else VectorOfDseRecordsJson(Vector())
        }

        val commentary:VectorOfCiteTriplesJson = {
          if (withCommentary) getCommentaryForText(reff) 
          else VectorOfCiteTriplesJson(Vector())
        }

        val v:Vector[Map[String,String]] = c.nodes.map(l => Map("urn" -> l.urn.toString, "text" -> l.text))
        val n:CorpusJson = CorpusJson(v, dseRecs, commentary) 
        Unmarshal(n).to[CorpusJson].map(Right(_))
      }
      case None => {
        //logger.info(s"got to case None")
        val passageComp:String = urn.passageComponentOption match {
          case Some(s) => s
          case None => ""
        }
        val allUrns:Vector[CtsUrn] = textRepository.get.corpus.citedWorks        
        val realUrns:Vector[CtsUrn] = allUrns.filter(urn.dropPassage == _.dropVersion)
        val corpora:Vector[Corpus] = realUrns.map(ru => textRepository.get.corpus >= CtsUrn(s"${ru.dropPassage}${passageComp}"))
        val assembledVector:Vector[Map[String,String]] = {
          (for (c <- corpora) yield {
            for (n <- c.nodes) yield {
              Map("urn" -> n.urn.toString, "text" -> n.text) 
            } 
          }).flatten
        }

          val corpUrns:Vector[CtsUrn] = corpora.map( c => {
            c.nodes.map( _.urn )
          }).flatten
          //logger.info(s"corpUrns: ${corpUrns}")
          // Get DSE records, if any, for the requested text
          val dseRecs:VectorOfDseRecordsJson = {
            if (withDse) dseRecordsComprehensive(corpUrns)
            else VectorOfDseRecordsJson(Vector())
          }

          val commentary:VectorOfCiteTriplesJson = {
            if (withCommentary) {
              getCommentaryForText(corpUrns)
            }
              else VectorOfCiteTriplesJson(Vector())
          }

          val n:CorpusJson = CorpusJson(assembledVector, dseRecs, commentary) 
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

def fetchFirstUrn(urnString: String): Future[Either[String,CtsUrnString]] = {
 try {
  val urn:CtsUrn = CtsUrn(urnString).dropPassage
  //if (urn.versionOption == None){ throw new ScsException("It is invalid to request 'firstUrn' on a notional work.") }
  val cn:CitableNode = textRepository.get.corpus.firstNode(urn)
  val urnReply = CtsUrnString(cn.urn.toString)
  Unmarshal(urnReply).to[CtsUrnString].map(Right(_))
} catch {
  case e: Exception => {
    Future.successful(Left(s"${new IOException(e)}"))
  }
}
}

def fetchPrevUrn(urnString: String): Future[Either[String,CtsUrnString]] = {
 try {
  val urn:CtsUrn = CtsUrn(urnString)
  if (urn.versionOption == None){ throw new ScsException("It is invalid to request 'prevUrn' on a notional work.") }
  val cn:Option[CtsUrn] = textRepository.get.corpus.prevUrn(urn)
  val urnReply = cn match {
    case Some(u) => CtsUrnString(u.toString)
    case None => CtsUrnString("")
  }
  Unmarshal(urnReply).to[CtsUrnString].map(Right(_))
} catch {
  case e: Exception => {
    Future.successful(Left(s"${new IOException(e)}"))
  }
}
}

def fetchNextUrn(urnString: String): Future[Either[String,CtsUrnString]] = {
 try {
  val urn:CtsUrn = CtsUrn(urnString)
  if (urn.versionOption == None){ throw new ScsException("It is invalid to request 'nextUrn' on a notional work.") }
  val cn:Option[CtsUrn] = textRepository.get.corpus.nextUrn(urn)
  val urnReply = cn match {
    case Some(u) => CtsUrnString(u.toString)
    case None => CtsUrnString("")
  }
  Unmarshal(urnReply).to[CtsUrnString].map(Right(_))
} catch {
  case e: Exception => {
    Future.successful(Left(s"${new IOException(e)}"))
  }
}
}

def fetchPrevText(urnString: String): Future[Either[String,CorpusJson]] = {
 try {
  val urn:CtsUrn = CtsUrn(urnString)
  if (urn.versionOption == None){ throw new ScsException("It is invalid to request 'prev' on a notional work.") }
  val vcn:Vector[CitableNode] = textRepository.get.corpus.prev(urn) 
  val v:Vector[Map[String,String]] = vcn.map(l => Map("urn" -> l.urn.toString, "text" -> l.text))
  val n:CorpusJson = CorpusJson(v) 
  Unmarshal(n).to[CorpusJson].map(Right(_))
} catch {
  case e: Exception => {
    Future.successful(Left(s"${new IOException(e)}"))
  }
}
}

def fetchNextText(urnString: String): Future[Either[String,CorpusJson]] = {
 try {
  val urn:CtsUrn = CtsUrn(urnString)
  if (urn.versionOption == None){ throw new ScsException("It is invalid to request 'next' on a notional work.") }
  val vcn:Vector[CitableNode] = textRepository.get.corpus.next(urn) 
  val v:Vector[Map[String,String]] = vcn.map(l => Map("urn" -> l.urn.toString, "text" -> l.text))
  val n:CorpusJson = CorpusJson(v) 
  Unmarshal(n).to[CorpusJson].map(Right(_))
} catch {
  case e: Exception => {
    Future.successful(Left(s"${new IOException(e)}"))
  }
}
}

def fetchFind(s:Vector[String], urnString:Option[String]): Future[Either[String,CorpusJson]] = {
  try {
    val findInCorpus:Corpus = urnString match {
      case Some(s) => {
        val urn:CtsUrn = CtsUrn(s)
        val newCorpus:Corpus = textRepository.get.corpus >= urn 
        newCorpus
      } 
      case None => textRepository.get.corpus
    } 
    val foundCorpus:Corpus = findInCorpus.find(s)
    val v:Vector[Map[String,String]] = foundCorpus.nodes.map(l => Map("urn" -> l.urn.toString, "text" -> l.text))
    val n:CorpusJson = CorpusJson(v) 
    Unmarshal(n).to[CorpusJson].map(Right(_))
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

  val nghv:NgramHistoJson = NgramHistoJson(ngh.histogram.map(h => ( Map[String,String]("s" -> h.s.toString, "count" -> h.count.toString) ) ))
  Unmarshal(nghv).to[NgramHistoJson].map(Right(_)) 

} catch {
  case e: Exception => {
    Future.successful(Left(s"${new IOException(e)}"))
  }
}
}

def fetchTokenFind(tokenString:String, urnString:Option[String], ignorePunctuation:Boolean = true): Future[Either[String, CorpusJson]] = {
  try {
    val findInCorpus:Corpus = urnString match {
      case Some(s) => {
        val urn:CtsUrn = CtsUrn(s)
        val newCorpus:Corpus = textRepository.get.corpus >= urn 
        newCorpus
      } 
      case None => textRepository.get.corpus
    } 
    val foundCorpus:Corpus = findInCorpus.findToken(tokenString, ignorePunctuation)
    val v:Vector[Map[String,String]] = foundCorpus.nodes.map(l => Map("urn" -> l.urn.toString, "text" -> l.text))
    val n:CorpusJson = CorpusJson(v) 
    Unmarshal(n).to[CorpusJson].map(Right(_))
  } catch {
    case e: Exception => {
      Future.successful(Left(s"${new IOException(e)}"))
    }
  }
}

def fetchAllTokensFind(tokenStrings:Vector[String], urnString:Option[String], ignorePunctuation:Boolean = true, dist:String = ""): Future[Either[String, CorpusJson]] = {

  try {
    // Let's see if there is a dist
    val distance:Option[Int] = if (dist == "") None else Some(dist.toInt)
    val findInCorpus:Corpus = urnString match {
      case Some(s) => {
        //logger.info("found URN")
        val urn:CtsUrn = CtsUrn(s)
        val newCorpus:Corpus = textRepository.get.corpus >= urn 
        newCorpus
      } 
      case None => textRepository.get.corpus
    } 
    val foundCorpus:Corpus = {
      distance match {
        case Some(d) => findInCorpus.findTokensWithin(tokenStrings, d, ignorePunctuation)
        case _ => findInCorpus.findTokens(tokenStrings, findInCorpus, ignorePunctuation)
      }
    }

    val v:Vector[Map[String,String]] = foundCorpus.nodes.map(l => Map("urn" -> l.urn.toString, "text" -> l.text))
    val n:CorpusJson = CorpusJson(v) 
    Unmarshal(n).to[CorpusJson].map(Right(_))
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
