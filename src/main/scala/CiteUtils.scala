package edu.furman.akkascs

import java.io.IOException
import java.io.File
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.math._
import scala.io.Source
import edu.holycross.shot.ohco2._
import edu.holycross.shot.cite._
import edu.holycross.shot.citeobj._
import edu.holycross.shot.scm._

//		  val cexData = Source.fromFile(cexFilePath).getLines.mkString("\n")
//		  val library = CiteLibrary(cexData,"#",",")

object CexRepos{ 
	def apply(cexFiles:List[java.io.File]):scala.collection.immutable.Map[String,CiteLibrary] = {
		val cexMap:scala.collection.immutable.Map[String,CiteLibrary] =
			cexFiles.foldLeft(Map[String,CiteLibrary]()){ (m, f) => m + (f.getName.replaceFirst(".cex","") ->
				CiteLibrary(Source.fromFile(f).getLines.mkString("\n"),"#",",")
			)
		}
		cexMap
	}
}

case class ScsException(extext:String) extends Exception
