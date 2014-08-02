package edu.umass.ciir.kbbridge.kb2text


import edu.umass.ciir.strepsi.StringTools
import scala.collection.JavaConversions._

/**
 * Extracts neighbors on the Wikipedia graph from the entity's document. In particular, gets inlinks and context links from
 * the metadata field and outlinks by parsing the &lt;link&gt; fields of the article xml.
 */
object WikiNeighborExtractor {
  case class WikiNeighbors(outLinks:Seq[WikiLinkExtractor.Anchor], inlinks:Seq[String], contextLinks:Map[String,Int])
  case class NeighborCount(sourceWikiTitle:String, targetWikiTitle:String, canonicalDestName:String, anchors: Seq[WikiLinkExtractor.Anchor], inlinkCount:Int, contextCount:Int)

  def documentNeighborCount(wikipediaTitle:String,  documentName:String, documentMeta:Map[String,String]):Seq[NeighborCount] = {
    val WikiNeighbors(outAnchors, inlinks, contextLinks) = findNeighbors(wikipediaTitle, documentName, documentMeta)

    val destinations = outAnchors.groupBy(_.destination)

    val neighborWithCounts  =
      for ((destination, anchors) <- destinations) yield {
        val inlinkCount = if (inlinks.contains(destination)) {1} else {0}
        val contextCount = contextLinks(destination)
        val canonicalDestName = WikiTools.wikititleToEntityName(destination)
        NeighborCount(wikipediaTitle, destination, canonicalDestName, anchors, inlinkCount, contextCount)
      }

    neighborWithCounts.toSeq
  }


  def passageNeighborCount(wikipediaTitle:String, documentName:String, documentText:String, documentMeta:Map[String,String], passageTextOpt:Option[String]):Seq[NeighborCount] = {

    val passageText = if(passageTextOpt.isDefined) passageTextOpt.get else documentText

    val WikiNeighbors(outAnchors, inlinks, contextLinks) = findNeighbors(wikipediaTitle,documentName, documentMeta)

    val passageLinks = outAnchors.filter(link => passageText.contains(link.rawAnchorText) || passageText.contains(link.anchorText))

    val destinations = passageLinks.groupBy(_.destination)

    val neighborWithCounts  =
      for ((destination, anchors) <- destinations) yield {
        val inlinkCount = if (inlinks.contains(destination)) {1} else {0}
        val contextCount = contextLinks(destination)
        val canonicalDestName = WikiTools.wikititleToEntityName(destination)
        NeighborCount(wikipediaTitle, destination, canonicalDestName, anchors, inlinkCount, contextCount)
      }

    neighborWithCounts.toSeq
  }
  def findNeighbors(thisWikiTitle:String, documentName:String, documentMeta:Map[String,String]):WikiNeighbors = {
    val outLinks = WikiLinkExtractor.simpleExtractorNoContext(documentName, documentMeta)
      .filterNot(anchor => (anchor.destination == thisWikiTitle) || ignoreWikiArticle(anchor.destination))
    val inLinks = srcInLinks(documentMeta)
    val contextLinks = contextLinkCoocurrences(documentMeta).toMap.withDefaultValue(0)
    WikiNeighbors(outLinks, inLinks, contextLinks)
  }


  def ignoreWikiArticle(destination:String):Boolean = {
    destination.startsWith("Category:") ||
      destination.startsWith("File:") ||
      destination.startsWith("List of ")
  }


  def srcInLinks(documentMeta:Map[String,String]):Seq[String] = {
    documentMeta.getOrElse("srcInlinks", "").split(" ")
  }

  def contextLinkCoocurrences(documentMeta:Map[String,String]):Seq[(String, Int)] = {
    for(line <- documentMeta.getOrElse("contextLinks", "").split("\n")) yield {
      val title = StringTools.getSplitChunk(line, 0).get
      val countOpt = StringTools.toIntOption(StringTools.getSplitChunk(line, 1).getOrElse("0"))
      title -> countOpt.getOrElse(0)
    }
  }

  def multiplyMapValue[K](m:Map[K,Double], scalar:Double):Map[K,Double] = {
    for((key,value) <- m) yield key -> (scalar * value)
  }



}



