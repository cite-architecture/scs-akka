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
  implicit val corpusFormat = jsonFormat1(CorpusJson.apply)
  implicit val citableNodeFormat = jsonFormat1(CitableNodeJson.apply)
  implicit val ngramHistoFormat = jsonFormat1(NgramHistoJson.apply)
  implicit val catalogFormat = jsonFormat1(CatalogJson.apply)
  implicit val reffFormat = jsonFormat1(ReffJson.apply)
}

trait Service extends Protocols with Ohco2Service with Ohco2Router with CiteObjectRouter {
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


/*
  val routes1 = {
    pathPrefix("ctsurn"  ) {
        (get & path(Segment)) { (urnString) =>
          complete {
            fetchCtsUrn(urnString).map[ToResponseMarshallable] {
              case Right(ctsUrnString) => ctsUrnString
              case Left(errorMessage) => BadRequest -> errorMessage
            }
          }
        }
      } 
  }

  val routes2 = {
    pathPrefix("object"  ) {
        (get & path(Segment)) { (urnString) =>
          complete {
            fetchCtsUrn(urnString).map[ToResponseMarshallable] {
              case Right(ctsUrnString) => ctsUrnString
              case Left(errorMessage) => BadRequest -> errorMessage
            }
          }
        }
      } 
  }
  val routes = {
    logRequestResult("cite-microservice") {
        routes1 ~routes2
    }
  }
  */

  val routes = ohco2Routes ~ citeObjectRoutes

  val routesX = {
    logRequestResult("cite-microservice") {
    pathPrefix("ctsurn"  ) {
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
      } 
    }
  }
}


object CiteMicroservice extends App with Service  {
  override implicit val system = ActorSystem()
  override implicit val executor = system.dispatcher
  override implicit val materializer = ActorMaterializer()

  override val config = ConfigFactory.load()
  override val logger = Logging(system, getClass)

  val textRepository:Option[TextRepository] = cexLibrary.textRepository 

  textRepository match {
    case Some(tr) => { 
        logger.debug(s"\n\nREADY\nCorpus-size: ${cexLibrary.textRepository.get.corpus.size}\n\n")
        cexLibrary.textRepository.get
      } 
    case None => {
        logger.debug(s"\n\nREADY\nNO TEXT REPOSITORY IN THIS CEX FILE!\n\n")
    }
  }

  Http().bindAndHandle(routes, config.getString("http.interface"), config.getInt("http.port"))

}
