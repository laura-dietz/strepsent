package edu.umass.ciir.kbbridge.kb2text

import scala.collection.mutable.ListBuffer
import scala.xml.{Node, NodeSeq, XML}
import scala.collection.JavaConversions._

/**
 * Parses wiki links from xml. Takes the input from the XML data and extractions the links from it.
 */
object WikiLinkXmlParser {

  case class Anchor(source: String, destination: String, anchorText: String, paragraphId: Int, rawAnchorText: String);



  def extractLinks(documentName:String, documentMeta:Map[String,String]): Seq[Anchor] = {

    val meta = documentMeta;
    var body = meta.getOrElse("xml", "");

    if (body != null && body.length>0) {
      body = body.replace("\\n", "\n")
    }

    val paragraphs =
      if (body == null || body == "\\N") NodeSeq.Empty
      else {
        try {
          val bodyXML = XML.loadString(body)
          (bodyXML \\ "paragraph") ++ (bodyXML \\ "list")
        } catch {
          case e: org.xml.sax.SAXParseException =>
            System.err.println("Article \"" + documentName + "\" has malformed XML in body:\n" + body, e.toString)
            NodeSeq.Empty
        }
      }

    // each paragraph maps into a tuple (wpid, title, AnchorArray[(String, String)]
    val links = paragraphs.zipWithIndex.map({
      case (paragraph, pIdx) => {
        val linksInParagraph = paragraph \\ "link"
        var outAnchors = linksInParagraph.map(link => extractLinkText(documentName, link, linksInParagraph, pIdx)).toArray
        outAnchors.filter(a => a.source != a.destination && a.destination.length() > 0 && a.anchorText.length() > 0).toSeq
      }
    }) // paragraphs.map returns a list of tuples per paragraph

    links.flatten.toSet.toSeq
  }

  def simpleExtractorNoContext(documentName:String, documentMeta:Map[String,String]): Seq[Anchor] = {
    val body = documentMeta.getOrElse("xml","")
    try {
      val bodyXML = XML.loadString(body.replace("\\n", "\n"))
      val links = bodyXML \\ "link"
      val outAnchors = links.map(link => extractAnchorFromLink(documentName, link)).filter(a => a.source != a.destination && a.destination.length() > 0 && a.anchorText.length() > 0)
      outAnchors
    } catch {
      case e: org.xml.sax.SAXParseException => extractLinks(documentName, documentMeta)
    }
  }

  def extractAnchorFromLink(src: String, n: Node) = {
    val destination = (n \ "target").text

    val p = (n \ "part").text
    val anchorText = if (p.length > 0) {
      p
    } else {
      destination
    }
    new Anchor(src, destination.replaceAll(" ", "_"), anchorText.replaceAll(",", "_"), -1, rawAnchorText = anchorText)
  }

  def extractLinkText(src: String, n: Node, context: scala.xml.NodeSeq, paragraphIdx: Int): Anchor = {

    var destination = (n \ "target").text
    var destinationTitle = destination.replaceAll(" ", "_")

    var contextPages = new ListBuffer[String]
    for (link <- context) {
      var target = (link \ "target").text.replaceAll(" ", "_")
      if (target.length() > 1 && !(destinationTitle equalsIgnoreCase target)) {
        contextPages += target
      }
    }

    // limit context to first 10 links
    contextPages = contextPages take 10

    var p = (n \ "part").text
    var anchorText = ""
    if (p.length > 0) {
      anchorText = p
    } else {
      anchorText = destination
    }
    new Anchor(src, destinationTitle, anchorText.replaceAll(",", "_"), paragraphIdx, rawAnchorText = anchorText)
  }

//  def main(args: Array[String]) {
//
//    val metadata = new HashMap[String, String];
//    val testEntity = new ScoredWikipediaEntity("Amherst_College",
//      5407,
//      0.0d,
//      1)
//    val document = DocumentBridgeMap.getKbDocumentProvider.getDocument("Amherst_College")
//    val links = simpleExtractorNoContext(document)
//    for (a <- links) {
//      println(a)
//    }
//  }

}

class JWikiLinkXmlParser {
  def extractLinkDestinations(documentName:String, documentMeta:java.util.Map[String,String]): java.util.List[String] = {
    val meta: Map[String, String] = scala.collection.JavaConversions.mapAsScalaMap(documentMeta).toMap
    new java.util.ArrayList[String](WikiLinkXmlParser.extractLinks(documentName, meta).map(_.destination))
  }
}

