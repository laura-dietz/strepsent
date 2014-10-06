package edu.umass.ciir.ede.facc

import java.io.PrintWriter
import java.util.regex.Pattern

import edu.umass.ciir.strepsent.{FreebaseEntityId, WikiEntityId}

import scala.collection.mutable

/**
 *Maps Freebase Ids to WikipediaTitles
 */
object Freebase2WikipediaMap {

  val (freebaseId2WikiTitleMap:Map[FreebaseEntityId, WikiEntityId], wikiTitle2freebaseIdMap:Map[WikiEntityId, FreebaseEntityId]) = {
    println("Loading freebase title map.")

    val f = io.Source.fromFile("./data/freebase-wiki-titles-new-clean")
    val freebaseId2WikiTitle = new mutable.HashMap[String,  String]()
    for(line <- f.getLines()) {
      val sp = line.split("\t")
      val freebaseId = sp(0)
      val wikipediaTitle = sp(1)
      freebaseId2WikiTitle += (freebaseId -> wikipediaTitle)
    }
    f.close()

    val fb2Wiki = freebaseId2WikiTitle.result().withDefault(freebaseId => "")
    val wiki2Fb = freebaseId2WikiTitle.result().map(pair => pair._2 -> pair._1).withDefault(wikiId => "")
    println("Done.")
    (fb2Wiki, wiki2Fb)
  }

  lazy val wikipediaTitleNormalization = freebaseId2WikiTitleMap.map(entry => (entry._2.toLowerCase, entry._2))


}

object FreebaseConverter extends App {

  val pw = new PrintWriter("./data/freebase-wiki-titles-new-clean")
  val f = io.Source.fromFile("./data/freebase-wiki-titles-new")
  for(line <- f.getLines()) {
    val sp = line.split("\t")
    val freebaseId = sp(0).replace("ns:", "").replace("m.", "/m/")
    try {
    val wikipediaTitle = decode(sp(2))
    pw.println(freebaseId + "\t" + wikipediaTitle)
    } catch {
      case _:Exception => println("Error decoding string: " + sp(2))
    }

  }
  pw.close()


  def decode( text : String) : String = {
    var newString = text
    val pattern = Pattern.compile("\\$([0-9,a-f,A-F]{4})")
    val unicodeMatcher = pattern.matcher(newString)
    while (unicodeMatcher.find()) {
      val charCode = Integer.parseInt(unicodeMatcher.group(1), 16)
      val string = if (charCode == 24) {
        "\\$"
      } else {
        "" + charCode.toChar
      }
      newString = unicodeMatcher.replaceFirst(string)
      unicodeMatcher.reset(newString)
    }
    newString = newString.replaceAll("\\\\\\\\\\\\", "")
    newString = newString.replaceAll("\\\\/", "/")
    newString = newString.substring(1, newString.length-2)
    newString
  }

}

