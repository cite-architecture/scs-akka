package edu.furman.akkascs

import akka.actor.ActorSystem
import akka.event.{LoggingAdapter, Logging}
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.model.{HttpResponse, HttpRequest}
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.unmarshalling.Unmarshal

import akka.http.scaladsl.model.HttpHeader
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.headers.`Access-Control-Allow-Credentials`
import akka.http.scaladsl.model.headers.`Access-Control-Allow-Methods`
import akka.http.scaladsl.model.headers.`Access-Control-Allow-Origin`
import akka.http.scaladsl.model.headers.`Access-Control-Allow-Headers`
import akka.http.scaladsl.model.headers.`Access-Control-Max-Age`
import akka.http.scaladsl.model.headers.Origin
import akka.http.scaladsl.server.Directive0
import akka.http.scaladsl.server.MethodRejection
import akka.http.scaladsl.server.RejectionHandler

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
import edu.holycross.shot.dse._
import edu.holycross.shot.citerelation._

import akka.http.scaladsl.model.MediaTypes.`application/json`

trait Protocols extends DefaultJsonProtocol {
  // Ohco2
  implicit val ctsUrnStringFormat = jsonFormat1(CtsUrnString.apply)
  implicit val cite2UrnStringFormat = jsonFormat1(Cite2UrnString.apply)
  implicit val citableNodeFormat = jsonFormat1(CitableNodeJson.apply)
  implicit val ngramHistoFormat = jsonFormat1(NgramHistoJson.apply)
  implicit val catalogFormat = jsonFormat1(CatalogJson.apply)
  implicit val reffFormat = jsonFormat1(ReffJson.apply)
  // CiteObject
  implicit val citeObjectFormat = jsonFormat2(CiteObjectJson.apply)
  implicit val ServiceUrlStringFormat = jsonFormat1(ServiceUrlString.apply)
  implicit val citePropertyDefFormat = jsonFormat1(CitePropertyDefJson.apply)
  implicit val objectLabelMapFormat = jsonFormat1(ObjectLabelMapJson.apply)
  implicit val citeCollectionPropertyDefsFormat = jsonFormat1(CiteCollectionPropertyDefsJson.apply)
  implicit val citeCollectionInfoFormat = jsonFormat1(CiteCollectionInfoJson.apply)
  implicit val citeCollectionDefFormat = jsonFormat2(CiteCollectionDefJson.apply)
  implicit val vectorOfCite2UrnsFormat = jsonFormat1(VectorOfCite2UrnsJson.apply)
  // CiteObjectJson
  implicit val vectorOfCiteCollectionDefsFormat = jsonFormat1(VectorOfCiteCollectionDefsJson.apply)
  // DataModel
  implicit val dataModelDefFormat = jsonFormat1(DataModelDefJson.apply)
  implicit val vectorOfDataModelDefFormat = jsonFormat1(VectorOfDataModelsDefJson.apply)
  // DSE 
  implicit val dseRecordFormat = jsonFormat5(DseRecordJson.apply)
  implicit val vectorOfDseRecordsFormat = jsonFormat1(VectorOfDseRecordsJson.apply)
  implicit val vectorOfCiteObjectsFormat = jsonFormat3(VectorOfCiteObjectsJson.apply)
  implicit val citeTripleFormat = jsonFormat3(CiteTripleJson.apply)
  implicit val vectorOfCiteTriplesFormat = jsonFormat1(VectorOfCiteTriplesJson.apply)
  implicit val corpusFormat = jsonFormat3(CorpusJson.apply)
}

trait Service extends Protocols with Ohco2Service with CiteCollectionService with CiteImageService with DseService with RelationsService with CorsSupport {
  implicit val system: ActorSystem
  implicit def executor: ExecutionContextExecutor
  implicit val materializer: Materializer

  implicit def cors[T]: Directive0
  implicit def corsAllowCredentials: Boolean 
  implicit def corsAllowOrigins: List[String] 
  implicit def corsAllowedHeaders: List[String] 
  implicit def optionsCorsHeaders: List[akka.http.scaladsl.model.HttpHeader] 

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
      logger.debug(s"\n\nUsing CEX file: ${f}\n")
      val cl:CiteLibrary = CiteLibrarySource.fromFile( f )
      cl
  }

  lazy val cexLibraryRemote:CiteLibrary = {
      val url:String = config.getString("cex.libraryUrl")
      logger.debug(s"\n\nUsing CEX file from URL: ${url}\n")
      //val cexFile = scala.io.Source.fromURL(url)
      //val cexString = cexFile.mkString
      val repo:CiteLibrary = CiteLibrarySource.fromUrl(url)
      repo
  }

  lazy val cexLibrary: CiteLibrary = {
    val useRemote:Boolean = config.getBoolean("cex.useRemote")
    val cl:CiteLibrary = useRemote match {
      case true => cexLibraryRemote
      case _ => cexLibraryLocal
    }
    cl
  }

  lazy val dseVector: DseVector = {
    DseVector.fromCiteLibrary(cexLibrary)
  }

  val routes = cors{
    {
      logRequestResult("cite-microservice") {

      pathPrefix("test"){
        // All-purpose tesing
        (get & path(Segment)) { (urnString) => 
          complete {
            fetchCiteCollectionDefJson(urnString).map[ToResponseMarshallable]{
              case Right(collDefString) => collDefString
              case Left(errorMessage) => BadRequest -> errorMessage
            }
          }
        }
      } ~
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
      pathPrefix("libraryinfo"  ) {
        (get) { 
          complete {
            fetchLibraryInfo.map[ToResponseMarshallable] {
              case Right(libraryInfoMap) => libraryInfoMap
              case Left(errorMessage) => BadRequest -> errorMessage
            }
          }
        }
      } ~
      pathPrefix("modelsforcollection"  ) {
        (get & path(Segment)) { urnString => 
          complete {
            modelsForCollection(urnString).map[ToResponseMarshallable] {
              case Right(cite2urns) => cite2urns
              case Left(errorMessage) => BadRequest -> errorMessage
            }
          }
        }
      } ~
      pathPrefix("collectionsformodel"  ) {
        (get & path(Segment)) { urnString => 
          complete {
            collectionsForModel(urnString).map[ToResponseMarshallable] {
              case Right(cite2urns) => cite2urns
              case Left(errorMessage) => BadRequest -> errorMessage
            }
          }
        }
      } ~
      pathPrefix("modelapplies"  ) {
        (get ) { 
          parameters('modelurn.as[String], 'collurn.as[String]) { (modelUrn, collUrn) => 
            complete {
              modelApplies(modelUrn, collUrn).map[ToResponseMarshallable] {
                case Right(b) => b.toString
                case Left(errorMessage) => BadRequest -> errorMessage
              }
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
        (get & path("label" / Segment)) { (urnString) =>
          complete {
            fetchLabelForUrn(urnString).map[ToResponseMarshallable] {
              case Right(nodeLabel) => nodeLabel
              case Left(errorMessage) => BadRequest -> errorMessage
            }
          }
        } ~ 
        (get & path( "ngram" / "urns"  )){
          parameters('ng.as[String]) { ng => 
            complete {
              fetchUrnsForNgramJson(None, ng).map[ToResponseMarshallable]{
                case Right(ngh) => ngh
                case Left(errorMessage) => BadRequest -> errorMessage
              }
            }
          }
        } ~
        (get & path( "ngram" / "urns" / "tocorpus"  )){
          parameters('ng.as[String]) { ng => 
            complete {
              urnsToKwikCorpus(None, ng).map[ToResponseMarshallable]{
                case Right(ngh) => ngh
                case Left(errorMessage) => BadRequest -> errorMessage
              }
            }
          }
        } ~  
        (get & path( "ngram" / "urns" / "tocorpus" / Segment )){ urnString =>
          parameters('ng.as[String]) { ng => 
            complete {
              urnsToKwikCorpus(Some(urnString), ng).map[ToResponseMarshallable]{
                case Right(ngh) => ngh
                case Left(errorMessage) => BadRequest -> errorMessage
              }
            }
          }
        } ~
        (get & path( "ngram" / "urns" / Segment )){ urnString =>
          parameters('ng.as[String]) { ng => 
            complete {
              fetchUrnsForNgramJson(Some(urnString), ng).map[ToResponseMarshallable]{
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
        (get & path( "find" )) { 
          parameters( 's.as[String].* ) { strings => 
            complete {
              val stringVector = strings.toVector
              fetchFind(stringVector, None).map[ToResponseMarshallable] {
                case Right(corpusString) => corpusString
                case Left(errorMessage) => BadRequest -> errorMessage
              }
            }
          }
        } ~
        (get & path( "find" / Segment )){ urnString =>
          parameters( 's.as[String].* ) { strings => 
            complete {
              val stringVector = strings.toVector
              fetchFind(stringVector, Some(urnString)).map[ToResponseMarshallable] {
                case Right(corpusString) => corpusString
                case Left(errorMessage) => BadRequest -> errorMessage
              }
            }
          }
        } ~
        (get & path( "token" / Segment )){ urnString =>
          parameters( 't.as[String], 'ignorePunctuation.as[Boolean] ? true ) { (tokenString, ignorePunctuation) => 
            complete {
              fetchTokenFind(tokenString, Some(urnString), ignorePunctuation).map[ToResponseMarshallable] {
                case Right(corpusString) => corpusString
                case Left(errorMessage) => BadRequest -> errorMessage
              }
            }
          }
        } ~
        (get & path( "token" )){ 
          parameters( 't.as[String], 'ignorePunctuation.as[Boolean] ? true ) { (tokenString, ignorePunctuation) => 
            complete {
              fetchTokenFind(tokenString, None, ignorePunctuation).map[ToResponseMarshallable] {
                case Right(corpusString) => corpusString
                case Left(errorMessage) => BadRequest -> errorMessage
              }
            }
          }
        } ~
         (get & path( "tokens" / Segment )){ urnString =>
          parameters( 't.as[String].*, 'ignorePunctuation.as[Boolean] ? true, 'dist.as[String] ? "" ) { (tokenStrings, ignorePunctuation, dist) => 
            complete {
              fetchAllTokensFind(tokenStrings.toVector, Some(urnString), ignorePunctuation, dist).map[ToResponseMarshallable] {
                case Right(corpusString) => corpusString
                case Left(errorMessage) => BadRequest -> errorMessage
              }
            }
          }
        } ~
        (get & path( "tokens" )){ 
          parameters( 't.as[String].*, 'ignorePunctuation.as[Boolean] ? true, 'dist.as[String] ? "" ) { (tokenStrings, ignorePunctuation, dist) => 
            complete {
              fetchAllTokensFind(tokenStrings.toVector, None, ignorePunctuation, dist).map[ToResponseMarshallable] {
                case Right(corpusString) => corpusString
                case Left(errorMessage) => BadRequest -> errorMessage
              }
            }
          }
        } ~
        (get & path(Segment)) { urnString =>
          parameters( 'dse.as[Boolean] ? false, 'commentary.as[Boolean] ? false ) { (withDse, withCommentary) =>
            complete {
              fetchOhco2Text(urnString, withDse, withCommentary).map[ToResponseMarshallable] {
                case Right(corpusString) => corpusString
                case Left(errorMessage) => BadRequest -> errorMessage
              }
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
        (get & path( "reff" / Segment)) { urnStr =>
          complete { 
            val urn:Cite2Urn = Cite2Urn(urnStr)
            fetchCollectionsReff(urn).map[ToResponseMarshallable]{
              case Right(objectVec) => objectVec
              case Left(errorMessage) => BadRequest -> errorMessage
            }              
          }
        } ~
        (get & path( "objects")) { 
          parameters( 'urn.as[String].* ) { strings => 
            complete { 
              val urns:Vector[Cite2Urn] = strings.toVector.map(s => Cite2Urn(s))
              fetchCiteObjectsFromNCollections(urns).map[ToResponseMarshallable]{
                case Right(objectVec) => objectVec
                case Left(errorMessage) => BadRequest -> errorMessage
              }              
            }
          }
        } ~
        (get & path( "labelmap")) { 
            complete { 
              fetchObjectsMapJson.map[ToResponseMarshallable]{
                case Right(labelMap) => labelMap
                case Left(errorMessage) => BadRequest -> errorMessage
              }              
          }
        } ~
        (get & path( Segment )) { urnString =>
          complete { 
            fetchCiteCollectionDefJson(urnString).map[ToResponseMarshallable]{
              case Right(citeCollectionDefJson) => citeCollectionDefJson
              case Left(errorMessage) => BadRequest -> errorMessage
            }
          }
        } ~
        (get & path( "hasobject" / Segment)) { urnString =>
          complete{
             hasObject(urnString).map[ToResponseMarshallable] {
              case Right(b) => b.toString
              case Left(errorMessage) => BadRequest -> errorMessage
             }
          }

        } ~
        (get & pathEndOrSingleSlash )  {
          complete {
            fetchVectorOfCiteCollectionDefsJson.map[ToResponseMarshallable]{
              case Right(citeObject) => citeObject
              case Left(errorMessage) => BadRequest -> errorMessage
            }
          }
        }
      } ~
      pathPrefix("datamodels") {
        (get & pathEndOrSingleSlash) {
          complete {
            fetchDataModelsJson.map[ToResponseMarshallable]{
              case Right(dataModel) => dataModel
              case Left(errorMessage) => BadRequest -> errorMessage
            }
          }
        }
      } ~
      pathPrefix("objects") {
        (get & path( "prevurn" / Segment)) { urnString => 
          complete { 
            fetchPrevCite2Urn(urnString).map[ToResponseMarshallable]{
              case Right(cite2UrnString) => cite2UrnString
              case Left(errorMessage) => BadRequest -> errorMessage
            }              
          }
        } ~
        (get & path( "nexturn" / Segment)) { urnString => 
          complete { 
            fetchNextCite2Urn(urnString).map[ToResponseMarshallable]{
              case Right(cite2UrnString) => cite2UrnString
              case Left(errorMessage) => BadRequest -> errorMessage
            }              
          }
        } ~
        (get & path( "paged" / Segment)) { urnString => 
          parameters('offset.as[Int] ? 1, 'limit.as[Int] ? 10, 'dse.as[Boolean] ? false) { (offset, limit, withDse) => 
            complete { 
              fetchPagedCiteObjectJson(urnString, offset, limit, withDse).map[ToResponseMarshallable]{
                case Right(citeObject) => citeObject
                case Left(errorMessage) => BadRequest -> errorMessage
              }              
            }
          }
        } ~
        (get & path("find" / "urnmatch" / Segment )) { urnString =>
          parameters( 'find.as[String], 'parameterurn.as[String] ?, 'offset.as[Int] ? , 'limit.as[Int] ? , 'dse.as[Boolean] ? false) { (urn, parameterUrn, offset, limit, withDse) => 
            //complete {s"'/find/urnmatch/${urnString}' with parameter ${urn} Not implemented yet."}
            complete {
              doUrnMatch(Some(urnString), urn, parameterUrn, offset, limit).map[ToResponseMarshallable]{
                case Right(citeObjects) => citeObjects
                case Left(errorMessage) => BadRequest -> errorMessage
              }              
            }
          }
        } ~
        (get & path("find" / "urnmatch" )) { 
          parameters( 'find.as[String], 'parameterurn.as[String] ?, 'offset.as[Int] ? , 'limit.as[Int] ? , 'dse.as[Boolean] ? false) { (urn, parameterUrn, offset, limit, withDse) => 
            complete {
              doUrnMatch(None, urn, parameterUrn, offset, limit, withDse).map[ToResponseMarshallable]{
                case Right(citeObjects) => citeObjects
                case Left(errorMessage) => BadRequest -> errorMessage
              }              
            }
          }
        } ~
        (get & path("find" / "regexmatch" / Segment )) { urnString =>
          parameters( 'find.as[String], 'parameterurn.as[String] ?, 'offset.as[Int] ? , 'limit.as[Int] ? ) { (rx, parameterUrn, offset, limit) => 
            complete {
              doRegexMatch(Some(urnString), rx, parameterUrn, offset, limit ).map[ToResponseMarshallable]{
                case Right(citeObjects) => citeObjects
                case Left(errorMessage) => BadRequest -> errorMessage
              }              
            }
          }
        } ~
        (get & path("find" / "regexmatch" )) { 
          parameters( 'find.as[String], 'parameterurn.as[String] ?, 'offset.as[Int] ? , 'limit.as[Int] ? ) { (rx, parameterUrn, offset, limit) => 
            complete {
              doRegexMatch(None, rx, parameterUrn, offset, limit).map[ToResponseMarshallable]{
                case Right(citeObjects) => citeObjects
                case Left(errorMessage) => BadRequest -> errorMessage
              }              
            }
          }
        } ~
        (get & path("find" / "stringcontains" / Segment )) { urnString =>
          parameters( 'find.as[String], 'casesensitive.as[Boolean] ? true , 'offset.as[Int] ? , 'limit.as[Int] ? ) { (s, caseSensitive, offset, limit) => 
            complete {
              doStringContains(Some(urnString), s, caseSensitive, offset, limit).map[ToResponseMarshallable]{
                case Right(citeObjects) => citeObjects
                case Left(errorMessage) => BadRequest -> errorMessage
              }              
            }
          }
        } ~
        (get & path("find" / "stringcontains" )) { 
          parameters( 'find.as[String], 'casesensitive.as[Boolean] ? true , 'offset.as[Int] ? , 'limit.as[Int] ? ) { (s, caseSensitive, offset, limit) => 
            complete {
              doStringContains(None, s, caseSensitive, offset, limit).map[ToResponseMarshallable]{
                case Right(citeObjects) => citeObjects
                case Left(errorMessage) => BadRequest -> errorMessage
              }              
            }
          }
        } ~
        (get & path("find" / "valueequals"  )) { 
          parameters( 'value.as[String], 'propertyurn.as[String] ?, 'type.as[String] ? , 'offset.as[Int] ? , 'limit.as[Int] ? ) { (valueToMatchStr, propertyUrnStr, typeStringOption, offset, limit) => 
            complete {
              doValueEquals(propertyUrnStr, valueToMatchStr, typeStringOption, offset, limit).map[ToResponseMarshallable]{
                case Right(citeObjects) => citeObjects
                case Left(errorMessage) => BadRequest -> errorMessage
              }
            }
          }
        } ~
        (get & path("find" / "numeric" / Segment )) { urnString =>
          parameters(  'n1.as[Double], 'op.as[String], 'n2.as[Double] ?, 'propertyurn.as[String] ?, 'offset.as[Int] ? , 'limit.as[Int] ?  ) { (n1, op, n2, parameterUrn, offset, limit) => 
            complete {
              doNumeric(Some(urnString), n1, op, n2, parameterUrn, offset, limit).map[ToResponseMarshallable]{
                case Right(citeObjects) => citeObjects
                case Left(errorMessage) => BadRequest -> errorMessage
              }
            }
          }
        } ~
        (get & path("find" / "numeric" )) { 
          parameters( 'n1.as[Double], 'op.as[String], 'n2.as[Double] ?,'propertyurn.as[String] ? , 'offset.as[Int] ? , 'limit.as[Int] ? ) { (n1, op, n2, parameterUrn, offset, limit) => 
            complete {
              doNumeric(None, n1, op, n2, parameterUrn, offset, limit).map[ToResponseMarshallable]{
                case Right(citeObjects) => citeObjects
                case Left(errorMessage) => BadRequest -> errorMessage
              }
            }
          }
        } ~
        (get & path(Segment)) { (urnString) =>
            parameters( 'dse.as[Boolean] ? false ){ withDse =>
              complete { 
                fetchCiteObjectJson(urnString, withDse).map[ToResponseMarshallable]{
                  case Right(citeObject) => citeObject
                  case Left(errorMessage) => BadRequest -> errorMessage
                }              
              }
            }
         } 
      } ~
      pathPrefix("image") {
        (get & path(Segment)) {  urnString =>
          parameters( 'resolveImage.as[Boolean] ? true ) { (resolveImage) => 
            val u:Cite2Urn = Cite2Urn(urnString)
            resolveImage match {
              case false => {
                complete {
                  iiifApiUrl(u).map[ToResponseMarshallable]{
                    case Right(su) => {
                      su
                    }
                    case Left(errorMessage) => BadRequest -> errorMessage
                  }
                }
              }
              case _ => {
                redirect(iiifApiResolver(urn = u),StatusCodes.TemporaryRedirect) 
              }
            }
          }
        } ~
        (get & path(Segment / Segment)) { (widthString, urnString) =>
          parameters( 'resolveImage.as[Boolean] ? true ) { (resolveImage) => 
            val u:Cite2Urn = Cite2Urn(urnString)
            //logger.info(s"urn = ${u}")
            val w:Int = widthString.toInt
            //logger.info(s"width = ${w}")
            resolveImage match {
              case false => {
                complete {
                  iiifApiUrl(urn = u, width = Some(w)).map[ToResponseMarshallable]{
                    case Right(su) => {
                      su
                    }
                    case Left(errorMessage) => BadRequest -> errorMessage
                  }
                }
              }
              case _ => {
                redirect(iiifApiResolver(urn = u, width = Some(w)),StatusCodes.TemporaryRedirect) 
              }
            }
          }
        } ~
        (get & path(Segment / Segment / Segment)) { (maxHeightString, maxWidthString, urnString) =>
          parameters( 'resolveImage.as[Boolean] ? true ) { (resolveImage) => 
            val u:Cite2Urn = Cite2Urn(urnString)
            val mw:Int = maxWidthString.toInt
            val mh:Int = maxHeightString.toInt
            resolveImage match {
              case false => {
                complete {
                  iiifApiUrl(urn = u, maxWidth = Some(mw), maxHeight = Some(mh)).map[ToResponseMarshallable]{
                    case Right(su) => {
                      su
                    }
                    case Left(errorMessage) => BadRequest -> errorMessage
                  }
                }
              }
              case _ => {
                redirect(iiifApiResolver(urn = u, maxWidth = Some(mw), maxHeight = Some(mh)),StatusCodes.TemporaryRedirect) 
              }
            }
          }
        } 
      } ~
      pathPrefix("relations") {
        (get & path( "verbs" )) {
          complete {
            relationsReturnVerbs.map[ToResponseMarshallable] {
              case Right(verbSet) => verbSet
              case Left(errorMessage) => BadRequest -> errorMessage
            }
          }
        } ~
        (get & path( "verbmap" )) {
          complete {
            relationsReturnVerbMap.map[ToResponseMarshallable] {
              case Right(verbMap) => verbMap
              case Left(errorMessage) => BadRequest -> errorMessage
            }
          }
        } ~
        (get & path(Segment)) { (urnString) =>
          parameters('filter.as[String] ? ) { filter => 
            complete {
              relationsReturnRelations(urnString, filter).map[ToResponseMarshallable] {
                case Right(citeTriples) => citeTriples
                case Left(errorMessage) => BadRequest -> errorMessage
              }
            }
          }
        }
      } ~
      pathPrefix("dse") {
        // Validate a CTS URN 
        (get & path( "test" / Segment)) { (urnString) =>
          complete {
            testDseService(urnString).map[ToResponseMarshallable] {
              case Right(ctsUrnString) => ctsUrnString
              case Left(errorMessage) => BadRequest -> errorMessage
            }
          }
        } ~
        (get & path( "recordsforimage" / Segment)) { (urnString) =>
          complete {
            dseRecordsForImage(urnString).map[ToResponseMarshallable] {
              case Right(ctsUrnString) => ctsUrnString
              case Left(errorMessage) => BadRequest -> errorMessage
            }
          }
        } ~
        (get & path( "recordsforsurface" / Segment)) { (urnString) =>
          complete {
            dseRecordsForSurface(urnString).map[ToResponseMarshallable] {
              case Right(ctsUrnString) => ctsUrnString
              case Left(errorMessage) => BadRequest -> errorMessage
            }
          }
        } ~  
        (get & path( "recordsfortext" / Segment)) { (urnString) =>
          complete {
            dseRecordsForText(urnString).map[ToResponseMarshallable] {
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

object CiteMicroservice extends App with Service with Ohco2Service with CiteCollectionService with CiteImageService with DseService with RelationsService with CorsSupport {
  override implicit val system = ActorSystem()
  override implicit val executor = system.dispatcher
  override implicit val materializer = ActorMaterializer()

  override val config = ConfigFactory.load()
  override val logger = Logging(system, getClass)

  override val corsAllowOrigins: List[String] = List("*","http://amphoreus.hpcc.uh.edu")

  override val corsAllowedHeaders: List[String] = List("Origin", "X-Requested-With", "Content-Type", "Accept", "Accept-Encoding", "Accept-Language", "Host", "Referer", "User-Agent")

  override val corsAllowCredentials: Boolean = true

  override val optionsCorsHeaders: List[HttpHeader] = List[HttpHeader](
    `Access-Control-Allow-Headers`(corsAllowedHeaders.mkString(", ")),
    `Access-Control-Max-Age`(60 * 60 * 24 * 20), // cache pre-flight response for 20 days
    `Access-Control-Allow-Credentials`(corsAllowCredentials)
  )


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
        logger.info(s"\n\nCorpus-size: ${tr.corpus.size}\n\n")
       // cexLibrary.textRepository.get
      } 
    case None => {
        logger.info(s"\n\nNO TEXT REPOSITORY IN THIS CEX FILE!\n\n")
    }
  }

  collectionRepository match {
    case Some(cr) => { 
        logger.info(s"\n\nCollection-size: ${cr.citableObjects.size}\n\n")
      } 
    case None => {
        logger.info(s"\n\nNO COLLECTION REPOSITORY IN THIS CEX FILE!\n\n")
    }
  }
  
  logger.info("Working on relations…")


  val numRelations:Int = {
    deluxeRelationSet match {
      case None => 0
      case Some(drs) => drs.relations.size
    }
  }

  logger.info(s"Deluxe RelationSet = ${numRelations} relations.") 


  Http().bindAndHandle(routes, config.getString("http.interface"), config.getInt("http.port"))


}
