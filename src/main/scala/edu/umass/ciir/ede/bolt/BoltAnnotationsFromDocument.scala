package edu.umass.ciir.ede.bolt

import scala.xml.XML
import edu.umass.ciir.ede.facc.{Freebase2WikipediaMap, FreebaseEntityAnnotation}
import scala.collection.mutable.ListBuffer
import edu.umass.ciir.ede.facc.FreebaseEntityAnnotation

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

  def extractFaccAnnotations(documentName: String, boltDoc:String) = {
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
    for(sent <- bodyXml \\ "sent"; if attr(sent,"@sid").toInt >= startSentId && attr(sent,"@sid").toInt <= endSentId){
      for(tok <- sent \\"token"){
        val thisStartChar = attr(tok, "@begin").toInt
        val thisEndChar = attr(tok, "@end").toInt
        startChar = math.min(startChar, thisStartChar)
        endChar = math.max(endChar, thisEndChar)
      }
    }

    def inCharSpan(spanBeginChar:Int, spanEndChar:Int):Boolean = {
      startChar <= spanBeginChar && spanEndChar <= endChar
    }

    val mentions = new scala.collection.mutable.HashMap[String, String]()
    for(mention <- bodyXml \\ "mention"; if inCharSpan(attr(mention,"@begin").toInt, attr(mention,"@end").toInt)){
      mentions += attr(mention,"@mid") -> mention.text
    }


    val annotations = new ListBuffer[FreebaseEntityAnnotation]()

    for(entity <- bodyXml \\ "entity"; if attr(entity, "@eid").startsWith("kb:")){
      for(mentref <- entity \\ "mentref"; if mentions.contains(attr(mentref,"@mid"))){
        // use this mention and this enitity
        val mentionText = mentions(attr(mentref,"@mid"))
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
                annotations += ann
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
