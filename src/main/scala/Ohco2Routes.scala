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
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.math._
import scala.util.{Success, Failure}
import scala.io.Source

import spray.json.DefaultJsonProtocol

import edu.holycross.shot.ohco2._
import edu.holycross.shot.cite._
import edu.holycross.shot.citeobj._
import edu.holycross.shot.scm._

trait Ohco2Router extends Protocols {
 def config: Config
 val logger: LoggingAdapter

	val ohco2Routes = {

		pathPrefix("ctsurn"  ) {
			(get & path(Segment)) { (urnString) =>
				complete {
					CiteMicroservice.fetchCtsUrn(urnString).map[ToResponseMarshallable] {
						case Right(ctsUrnString) => ctsUrnString
						case Left(errorMessage) => BadRequest -> errorMessage
						}
					}
				}
			} 	
		}
	}