package edu.umass.ciir.ede.bolt

import scala.xml.XML
import edu.umass.ciir.strepsi.trec.QueryFileLoader

/**
 * User: dietz
 * Date: 2/12/14
 * Time: 8:22 PM
 */
object BoltQueryFileLoader extends QueryFileLoader {
  def loadTsvQueries(queryFilename: String, prefix:String="")  : Map[Int, String] = {
    val data = XML.load(new java.io.InputStreamReader(new java.io.FileInputStream(queryFilename), "UTF-8"))
    val queries =
      for(topic <- data \\ "topic") yield {
        val queryId =( topic \ "@number").text
        val queryString = (topic \ "query").text

        queryId.replace(prefix, "").toInt -> queryString

      }

    queries.toMap

  }


}

