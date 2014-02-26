package edu.umass.ciir.ede.elannotation

import java.io.{PrintWriter, File}
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/**
 * Created by jdalton on 1/21/14.
 */
object StatsCreator extends App {

  val annotationDir = "/usr/aubury/scratch1/jdalton/data/robust04-nlp-annotations-factkb1"

  val countsMap = mutable.HashMap[String, (Long, Long)]()

  var fileCount = 0
  for (f <- new File(annotationDir).listFiles()) {
    if (fileCount%100==0) {
      println(s"$fileCount\t${f.getName}")
    }
    val docId = f.getName.replace(".xml", "")
    try {
      val linkedEntities = EntityAnnotationLoader.entityLinks(docId, f)
      val linkedMentions = linkedEntities.map(_.entityLinks take 3).flatten.filter(_.score > 0.5)
      val wikiTitles = linkedMentions.map(_.wikipediaTitle)
      val counts = wikiTitles.groupBy(identity).mapValues(_.length).toSeq
      for ((entity, count) <- counts) {
        val (cfCount, dfCount) = countsMap.getOrElse(entity, (0L, 0L))
        val updatedCfCount = cfCount + count
        val updatedDfCount = dfCount + 1
        countsMap.update(entity, (updatedCfCount,updatedDfCount))
      }

    } catch {
      case e: Exception => println(("error processing doc: " + docId + " " + e.getMessage))
    }
    fileCount+=1
  }

  val pw = new PrintWriter("./data/stats/robust-annotations")
  for ((entity, (cfCount, dfCount)) <- countsMap) {
    pw.println(s"$entity\t$cfCount\t$dfCount")
  }
  pw.close()


}
