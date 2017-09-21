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

import spray.json.DefaultJsonProtocol

import edu.holycross.shot.ohco2._
import edu.holycross.shot.cite._
import edu.holycross.shot.citeobj._
import edu.holycross.shot.scm._



trait Protocols extends DefaultJsonProtocol {
  implicit val ctsUrnStringFormat = jsonFormat1(CtsUrnString.apply)
  implicit val corpusNodesFormat = jsonFormat1(CitableNodesJson.apply)
}

trait Service extends Protocols with Ohco2Service {
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
      logger.info(s"\n\nCEX files at: ${cexDirectory}\n")
      val cexFiles:List[java.io.File] = getListOfFiles(cexDirectory)
      val cl:scala.collection.immutable.Map[String,CiteLibrary] = CexRepos(cexFiles)
      cl
  }

  lazy val cexLibrary:CiteLibrary = {
      val f:String = config.getString("cex.library")
      logger.info(s"\n\nUsing CEX file: ${f}\n")
      val cl:CiteLibrary = CiteLibrarySource.fromFile( f , "#", ",")
      cl
  }

  val routes = {
    logRequestResult("cite-microservice") {
    pathPrefix("urn"  ) {
        (get & path(Segment)) { (urnString) =>
          complete {
            logger.info(s"\n\nWill check ${urnString} for validity.")
            fetchCtsUrn(urnString).map[ToResponseMarshallable] {
              case Right(ctsUrnString) => ctsUrnString
              case Left(errorMessage) => BadRequest -> errorMessage
            }
          }
        }
      } ~
      pathPrefix("texts"  ) {
        (get & path(Segment)) { urnString =>
          complete {
            fetchOhco2Text(urnString).map[ToResponseMarshallable] {
              case Right(corpusString) => corpusString
              case Left(errorMessage) => BadRequest -> errorMessage
            }
          }
        } ~
        (get & path("first" / Segment)) { (urnString) =>
          complete {
            logger.info(s"\n\nWill deliver FIRST citable node for ${urnString}.\n")
            fetchCtsUrn(urnString).map[ToResponseMarshallable] {
              case Right(ctsUrnString) => ctsUrnString
              case Left(errorMessage) => BadRequest -> errorMessage
            }
          }
        } ~
        (get & path( "ngram" / )) { 
          parameters('n.as[Int], 'urn.as[String]) { (n, u) =>
            complete {
              logger.info(s"\n\nWill deliver ngram histogram with n=${n}.\n")
              fetchCtsUrn("urn:cts:greekLit:tlg0012.tlg001:1.1").map[ToResponseMarshallable] {
                case Right(ctsUrnString) => ctsUrnString
                case Left(errorMessage) => BadRequest -> errorMessage
              }
            }
          }
        }
      } 
    }
  }
}


object CiteMicroservice extends App with Service with Ohco2Service {
  override implicit val system = ActorSystem()
  override implicit val executor = system.dispatcher
  override implicit val materializer = ActorMaterializer()

  override val config = ConfigFactory.load()
  override val logger = Logging(system, getClass)

  logger.debug(s"\n\nREADY\nCorpus-size: ${cexLibrary.textRepository.get.corpus.size}\n\n")

  Http().bindAndHandle(routes, config.getString("http.interface"), config.getInt("http.port"))
}
