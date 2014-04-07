package edu.umass.ciir.ede.bolt

import scala.xml.XML
import edu.umass.ciir.ede.facc.{Freebase2WikipediaMap, FreebaseEntityAnnotation}
import scala.collection.mutable.ListBuffer
import edu.umass.ciir.ede.facc.FreebaseEntityAnnotation
import scala.collection.mutable

/**
 * Read post-level entity annotations from IBM-annotated Bolt threats
 */
class BoltAnnotationsFromDocument(tacId2WikiTitleMap:Map[String,String]) {

  //  def extractKbLinks(document: Document, boltDoc:String) = {
  //
  //    val fixedBody = "<document>" + document.text + "</document>"
  //    val bodyXML = XML.loadString(fixedBody)
  //    val links = bodyXML \\ "kbLinks"
  //    links.map(_.text)
  //  }
  //
  private def attr(n:scala.xml.Node, attrStr:String):String = (n \ attrStr).text

  def extractFaccAnnotations(documentName: String, boltDoc:String):ListBuffer[FreebaseEntityAnnotation] = {
    extractFaccAnnotationsSent(documentName, boltDoc).map(_._1)
  }
  def extractFaccAnnotationsSent(documentName: String, boltDoc:String):ListBuffer[(FreebaseEntityAnnotation,Int)] = {
    val postId = documentName.substring(documentName.lastIndexOf("_p")+1)
    val bodyXml = XML.loadString(boltDoc)

    // find start/end sentence for this post
    var startSentId = Int.MaxValue
    var endSentId = Int.MinValue


    for(sent_info_block <-  bodyXml\\"sgm_tag_sent_info"; if attr(sent_info_block,"@tag")== "post"){
      for(sent_info <- sent_info_block\\ "sgm_tag_sent_span"; if attr(sent_info, "@id") == postId){
        val thisStartSentId = attr(sent_info ,"@start").toInt
        val thisEndSentId = attr(sent_info,"@end").toInt
        startSentId =  math.min(thisStartSentId, startSentId)
        endSentId =  math.max(thisEndSentId, endSentId)
      }
    }


    var startChar = Int.MaxValue
    var endChar = Int.MinValue
    val startChar2Sent = new mutable.HashMap[Int, Int]()

    for(sent <- bodyXml \\ "sent"; if attr(sent,"@sid").toInt >= startSentId && attr(sent,"@sid").toInt <= endSentId){
      val sentId = attr(sent,"@sid").toInt
      for(tok <- sent \\"token"){
        val thisStartChar = attr(tok, "@begin").toInt
        val thisEndChar = attr(tok, "@end").toInt
        startChar = math.min(startChar, thisStartChar)
        endChar = math.max(endChar, thisEndChar)
        startChar2Sent += thisStartChar -> sentId
      }
    }

    def inCharSpan(spanBeginChar:Int, spanEndChar:Int):Boolean = {
      startChar <= spanBeginChar && spanEndChar <= endChar
    }

    val mentions = new scala.collection.mutable.HashMap[String, (String, Int)]()
    for(mention <- bodyXml \\ "mention"; if inCharSpan(attr(mention,"@begin").toInt, attr(mention,"@end").toInt)){
      val startChar = attr(mention, "@begin").toInt
      val mid = attr(mention, "@mid")
      val sentId = startChar2Sent.getOrElse(startChar, {throw new RuntimeException("could not find sentence Id starting with "+startChar+ " in mapping "+startChar2Sent)})
      mentions += mid -> (mention.text, sentId)
    }


    val annotations = new ListBuffer[(FreebaseEntityAnnotation, Int)]()

    for(entity <- bodyXml \\ "entity"; if attr(entity, "@eid").startsWith("kb:")){
      for(mentref <- entity \\ "mentref"; if mentions.contains(attr(mentref,"@mid"))){
        // use this mention and this enitity
        val (mentionText, mentionSentId) = mentions(attr(mentref,"@mid"))
        val tacId = attr(entity,"@eid").substring("kb:".length)
        val wikititleOpt= tacId2WikiTitleMap.get(tacId)

        // ignore entities with lookup failure
        wikititleOpt match {
          case Some(wikititle) =>  {
            val freebaseIdOpt = Freebase2WikipediaMap.wikiTitle2freebaseIdMap.get(wikititle)
            freebaseIdOpt match {
              case Some(freebaseId) => {
                val entityScore = attr(entity, "@score").toDouble

                val ann = FreebaseEntityAnnotation(documentName, "UTF-8", mentionText, -1,-1,entityScore, entityScore, freebaseId)
                annotations += ann -> mentionSentId
              }
              case _ => {}
            }
          }
          case _ => {}
        }
      }
    }

    annotations

  }

  def isWikiDoc(docId:String):Boolean = {
    false
  }

  def hasFaccAnnotation(documentName:String, boltDoc:String):Boolean = {
    boltDoc.length() > 10
  }

}
