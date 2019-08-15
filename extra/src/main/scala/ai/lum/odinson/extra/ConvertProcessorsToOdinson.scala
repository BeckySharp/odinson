package ai.lum.odinson.extra

import java.io.File
import scala.util.{ Failure, Success, Try }
import com.typesafe.scalalogging.LazyLogging
import org.clulab.processors.{ Document => ProcessorsDocument }
import org.clulab.utils.Serializer
import ai.lum.common.FileUtils._
import ai.lum.common.ConfigUtils._
import ai.lum.common.ConfigFactory
import ai.lum.odinson.Document


object ConvertProcessorsToOdinson extends App with LazyLogging {

  val config = ConfigFactory.load()
  val textDir: File = config[File]("odinson.textDir")
  val docsDir: File = config[File]("odinson.docsDir")

  // create output directory if it does not exist
  if (!docsDir.exists) {
    logger.warn(s"Making directory $docsDir")
    docsDir.mkdirs()
  }

  def deserializeProcessors(f: File): Document = {
    val doc = Serializer.load[ProcessorsDocument](f)
    ProcessorsUtils.convertDocument(doc)
  }

  // NOTE parses the documents in parallel
  for (f <- textDir.listFilesByWildcard("*.ser", caseSensitive = false, recursive = true).toSeq.par) {
    val docFile = new File(docsDir, f.getBaseName() + ".json")

    if (docFile.exists) {
      logger.warn(s"${docFile.getCanonicalPath} already exists")
    } else {
      Try {
        val doc = deserializeProcessors(f)
        docFile.writeString(doc.toJson)
      } match {
        case Success(_) =>
          logger.info(s"Deserialized ${f.getCanonicalPath}")
        case Failure(e) =>
          logger.error(s"Failed to deserialize ${f.getName}", e)
      }
    }
  }

}
