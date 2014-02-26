package edu.umass.ciir.ede.facc

import java.io.{PrintWriter, BufferedInputStream, FileInputStream}
import java.util.zip.GZIPInputStream
import scala.io.Source
import java.util.regex.Pattern

/**
 * Created with IntelliJ IDEA.
 * User: jdalton
 * Date: 8/28/13
 * Time: 12:34 PM
 * To change this template use File | Settings | File Templates.
 */
object FreebaseDataReader extends App {

  val freebaseDumpFile = "/home/jdalton/scratch1/data/freebase-rdf-2013-08-04-00-00.gz"
  val pw = new PrintWriter("./data/new-free-wiki-titles")
  val src = Source.fromInputStream(new GZIPInputStream(new BufferedInputStream(new FileInputStream(freebaseDumpFile))))
  var numFound = 0
  for (line <- src.getLines()) {
    if (line.contains("key:wikipedia.en_title")) {
      if (numFound % 1000 == 0) {
        println(numFound +"\t" + line)
      }
      numFound += 1

      val sp = line.split("\t")
      val freebaseId = sp(0).replace("ns:", "").replace("m.", "/m/")
      try {
        val wikipediaTitle = decode(sp(2))
        pw.println(freebaseId + "\t" + wikipediaTitle)
      } catch {
        case _:Exception => println("Error decoding string: " + sp(2))
      }
    }
  }
  pw.close()
  src.close()

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
