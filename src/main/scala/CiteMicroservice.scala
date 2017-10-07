package edu.furman.akkascs

import akka.actor.ActorSystem
import akka.event.{LoggingAdapter, Logging}
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.{HttpResponse, HttpRequest}
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.{ActorMaterializer, Materializer}
import akka.stream.scaladsl.{Flow, Sink, Source}
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import java.io.IOException
import java.io.File

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.math._
import scala.util.{Success, Failure}
import scala.io.Source

import spray.json.DefaultJsonProtocol

import edu.holycross.shot.ohco2._
import edu.holycross.shot.cite._
import edu.holycross.shot.citeobj._
import edu.holycross.shot.scm._


trait Protocols extends DefaultJsonProtocol {
  implicit val ctsUrnStringFormat = jsonFormat1(CtsUrnString.apply)
  implicit val cite2UrnStringFormat = jsonFormat1(Cite2UrnString.apply)
  implicit val corpusFormat = jsonFormat1(CorpusJson.apply)
  implicit val citableNodeFormat = jsonFormat1(CitableNodeJson.apply)
  implicit val ngramHistoFormat = jsonFormat1(NgramHistoJson.apply)
  implicit val catalogFormat = jsonFormat1(CatalogJson.apply)
  implicit val reffFormat = jsonFormat1(ReffJson.apply)
  implicit val citeObjectFormat = jsonFormat1(CiteObjectJson.apply)
  //implicit val citeCatalogFormat = jsonFormat1(CiteCatalogJson.apply)
  implicit val citePropertyDefFormat = jsonFormat1(CitePropertyDefJson.apply)
  implicit val citeCollectionDefFormat = jsonFormat1(CiteCollectionDefJson.apply)
}

trait Service extends Protocols with Ohco2Service with CiteCollectionService {
  implicit val system: ActorSystem
  implicit def executor: ExecutionContextExecutor
  implicit val materializer: Materializer

  def config: Config
  val logger: LoggingAdapter

  def getListOfFiles(dir: String):List[File] = {
    val d = new File(dir)
    if (d.exists && d.isDirectory) {
      d.listFiles.filter(_.isFile).toList
    } else {
      List[File]()
    }
  }

  lazy val cexLibraries:scala.collection.immutable.Map[String,CiteLibrary] = {
      val cexDirectory:String = config.getString("cex.directory")  
      val cexFiles:List[java.io.File] = getListOfFiles(cexDirectory)
      val cl:scala.collection.immutable.Map[String,CiteLibrary] = CexRepos(cexFiles)
      cl
  }

  lazy val cexLibraryLocal:CiteLibrary = {
      val f:String = config.getString("cex.library")
      logger.info(s"\n\nUsing CEX file: ${f}\n")
      val cl:CiteLibrary = CiteLibrarySource.fromFile( f , "#", ",")
      cl
  }

  lazy val cexLibraryRemote:CiteLibrary = {
      val url:String = config.getString("cex.libraryUrl")
      logger.info(s"\n\nUsing CEX file from URL: ${url}\n")
      val cexFile = scala.io.Source.fromURL(url)
      val cexString = cexFile.mkString
      val repo:CiteLibrary = CiteLibrary(cexString, "#", ",")
      repo
  }

  lazy val cexLibrary:CiteLibrary = {
    val useRemote:Boolean = config.getBoolean("cex.useRemote")
    val cl:CiteLibrary = useRemote match {
      case true => cexLibraryRemote
      case _ => cexLibraryLocal
    }
    cl
  }

  val routes = {
    logRequestResult("cite-microservice") {

    pathPrefix("ctsurn"  ) {
    // Validate a CTS URN 
        (get & path(Segment)) { (urnString) =>
          complete {
            fetchCtsUrn(urnString).map[ToResponseMarshallable] {
              case Right(ctsUrnString) => ctsUrnString
              case Left(errorMessage) => BadRequest -> errorMessage
            }
          }
        }
      } ~
      pathPrefix("textcatalog") {
      // Get Text Catalog
        (get & path( Segment)) { urnString => 
          complete {
            fetchCatalog(Some(urnString)).map[ToResponseMarshallable] {
              case Right(catalogString) => catalogString
              case Left(errorMessage) => BadRequest -> errorMessage
            }
          }
        } ~
        (get & pathEndOrSingleSlash ) { 
          complete {
            fetchCatalog(None).map[ToResponseMarshallable] {
              case Right(catalogString) => catalogString
              case Left(errorMessage) => BadRequest -> errorMessage
            }
          }
        }
      } ~
      pathPrefix("reff") {
      // Reff
        (get & path(Segment)) { urnString =>
          complete {
            fetchReff(urnString).map[ToResponseMarshallable] {
              case Right(reffString) => reffString
              case Left(errorMessage) => BadRequest -> errorMessage
            }
          } 
        }
      } ~
      pathPrefix("texts"  ) {
      // All the '/texts' routes
        (get & path("first" / Segment)) { (urnString) =>
          complete {
            fetchFirstNode(urnString).map[ToResponseMarshallable] {
              case Right(citableNodeString) => citableNodeString
              case Left(errorMessage) => BadRequest -> errorMessage
            }
          }
        } ~
        (get & path("firsturn" / Segment)) { (urnString) =>
          complete {
            fetchFirstUrn(urnString).map[ToResponseMarshallable] {
              case Right(ctsUrnString) => ctsUrnString
              case Left(errorMessage) => BadRequest -> errorMessage
            }
          }
        } ~
        (get & path("prev" / Segment)) { (urnString) =>
          complete {
            fetchPrevText(urnString).map[ToResponseMarshallable] {
              case Right(corpusString) => corpusString
              case Left(errorMessage) => BadRequest -> errorMessage
            }
          }
        } ~
        (get & path("prevurn" / Segment)) { (urnString) =>
          complete {
            fetchPrevUrn(urnString).map[ToResponseMarshallable] {
              case Right(ctsUrnString) => ctsUrnString
              case Left(errorMessage) => BadRequest -> errorMessage
            }
          }
        } ~
        (get & path("next" / Segment)) { (urnString) =>
          complete {
            fetchNextText(urnString).map[ToResponseMarshallable] {
              case Right(corpusString) => corpusString
              case Left(errorMessage) => BadRequest -> errorMessage
            }
          }
        } ~
        (get & path("nexturn" / Segment)) { (urnString) =>
          complete {
            fetchNextUrn(urnString).map[ToResponseMarshallable] {
              case Right(ctsUrnString) => ctsUrnString
              case Left(errorMessage) => BadRequest -> errorMessage
            }
          }
        } ~ 
        (get & path( "ngram" / "urns"  )){
          parameters('ng.as[String]) { ng => 
            complete {
              fetchUrnsForNgram(None, ng).map[ToResponseMarshallable]{
                case Right(ngh) => ngh
                case Left(errorMessage) => BadRequest -> errorMessage
              }
            }
          }
        } ~
        (get & path( "ngram" / "urns" / Segment )){ urnString =>
          parameters('ng.as[String]) { ng => 
            complete {
              fetchUrnsForNgram(Some(urnString), ng).map[ToResponseMarshallable]{
                case Right(ngh) => ngh
                case Left(errorMessage) => BadRequest -> errorMessage
              }
            }
          }
        } ~
        (get & path( "ngram" / Segment )){ urnString =>
          parameters('n.as[Int] ? 3, 't.as[Int] ? 1, 's.as[String] ?, 'ignorePunctuation.as[Boolean] ? true) { (n, t, s, ignorePunctuation) =>
            complete {
              fetchNgram(n, t, s, Some(urnString), ignorePunctuation).map[ToResponseMarshallable] {
                case Right(ngh) => ngh
                case Left(errorMessage) => BadRequest -> errorMessage
              }
            }
          }
        } ~
        (get & path( "ngram" )) { 
          parameters('n.as[Int] ? 3, 't.as[Int] ? 1, 's.as[String] ?, 'ignorePunctuation.as[Boolean] ? true) { (n, t, s, ignorePunctuation) =>
            complete {
              fetchNgram(n, t, s, None, ignorePunctuation).map[ToResponseMarshallable] {
                case Right(ngh) => ngh
                case Left(errorMessage) => BadRequest -> errorMessage
              }
            }
          }
        } ~
        (get & path(Segment)) { urnString =>
          complete {
            fetchOhco2Text(urnString).map[ToResponseMarshallable] {
              case Right(corpusString) => corpusString
              case Left(errorMessage) => BadRequest -> errorMessage
            }
          }
        } ~ 
        (get &  pathEndOrSingleSlash ) { 
          complete {
            fetchCatalog(None).map[ToResponseMarshallable] {
              case Right(catalogString) => catalogString
              case Left(errorMessage) => BadRequest -> errorMessage
            }
          }
        }  
      } ~
      pathPrefix("cite2urn"){
        // Validate a Cite2 URN
        (get & path(Segment)) { (urnString) =>
          complete {
            fetchCite2Urn(urnString).map[ToResponseMarshallable] {
              case Right(cite2UrnString) => cite2UrnString
              case Left(errorMessage) => BadRequest -> errorMessage
            }
          }
        }
      } ~
      pathPrefix("collections") {
        (get & path("def" / Segment )) { urnString =>
          complete { 
            fetchCiteCollectionDef(urnString).map[ToResponseMarshallable]{
              case Right(citeCollectionDefJson) => citeCollectionDefJson
              case Left(errorMessage) => BadRequest -> errorMessage
            }
          }
        }
      } ~
      pathPrefix("object") {
        (get & path(Segment)) { (urnString) =>
            complete { 
              fetchCiteObject(urnString).map[ToResponseMarshallable]{
              case Right(citeObject) => citeObject
              case Left(errorMessage) => BadRequest -> errorMessage
              }              
            }
         }
      } ~
      pathPrefix("extensions") {
        complete { "'/extensions' Not implemented yet."}
      }
    }
  }
}


object CiteMicroservice extends App with Service with Ohco2Service with CiteCollectionService {
  override implicit val system = ActorSystem()
  override implicit val executor = system.dispatcher
  override implicit val materializer = ActorMaterializer()

  override val config = ConfigFactory.load()
  override val logger = Logging(system, getClass)

  logger.debug(s"\n\nSTARTING…\n")

  
  logger.debug(s"\nGETTING TEXTS…\n")
  val textRepository:Option[TextRepository] = cexLibrary.textRepository 
  logger.debug(s"\nGETTING COLLECTIONS…\n")
  val collectionRepository:Option[CiteCollectionRepository] = cexLibrary.collectionRepository 
  logger.debug(s"\nGETTING COLLECTION OBJECTS…\n")
  val citableObjects:Option[Vector[CiteObject]] = collectionRepository match {
    case Some(cr) => Some(collectionRepository.get.citableObjects)
    case None => None
  }
  logger.debug(s"\nDONE GETTING COLLECTION OBJECTS…\n")



  textRepository match {
    case Some(tr) => { 
        logger.debug(s"\n\nCorpus-size: ${tr.corpus.size}\n\n")
       // cexLibrary.textRepository.get
      } 
    case None => {
        logger.debug(s"\n\nNO TEXT REPOSITORY IN THIS CEX FILE!\n\n")
    }
  }

  collectionRepository match {
    case Some(cr) => { 
        logger.debug(s"\n\nCollection-size: ${cr.citableObjects.size}\n\n")
      } 
    case None => {
        logger.debug(s"\n\nNO COLLECTION REPOSITORY IN THIS CEX FILE!\n\n")
    }
  }

  Http().bindAndHandle(routes, config.getString("http.interface"), config.getInt("http.port"))

}
