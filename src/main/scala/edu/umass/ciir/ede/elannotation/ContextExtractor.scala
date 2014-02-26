package edu.umass.ciir.ede.elannotation

import java.io.File
import scala.xml.XML
//import edu.umass.ciir.kbbridge.data.{ScoredWikipediaEntity, SimpleEntityMention}

/**
 * Created by jdalton on 1/21/14.
 */
object ContextExtractor {

  def extractEntityWindowContext(tokenWindowSize:Int, aDoc : AnnotatedDocument) = {
      val contexts = for (kbLink <- aDoc.kbLinks) yield {
        val start = kbLink.tokenBegin
        val end = kbLink.tokenEnd

        val startWindow = Math.max(start - (tokenWindowSize/2),0)
        val endWindow = Math.min(end + (tokenWindowSize/2), aDoc.tokens.length)
        val tokens = aDoc.tokens.slice(startWindow, endWindow)
        val neighboringLinks = aDoc.kbLinks.filter(link => link.tokenBegin >= startWindow && link.tokenBegin <= endWindow && link.tokenBegin != start)
        kbLink -> (tokens, neighboringLinks)
      }
    contexts
  }


  def extractEntityContextSentence(aDoc : AnnotatedDocument) = {

    val contexts = for (kbLink <- aDoc.kbLinks) yield {
      val (startSent, endSent) = extractLinkSentenceContext(kbLink, aDoc.tokens)
      val sentenceTokens = aDoc.tokens.slice(startSent, endSent)
      val neighboringSentenceLinks = aDoc.kbLinks.filter(link => link.tokenBegin >= startSent && link.tokenBegin <= endSent && link.tokenBegin != kbLink.tokenBegin)

      kbLink -> (sentenceTokens, neighboringSentenceLinks)
    }
    contexts
  }



  def extractLinkSentenceContext(kbLink : LinkedMention, tokens: IndexedSeq[Token]) = {
    val start = kbLink.tokenBegin
    val end = kbLink.tokenEnd

    // backwards
    val startSent = findStartSent(tokens, start, 0, -1)

    // forwards
    val endSent = findStartSent(tokens, end, tokens.length-1, 1)

    (startSent, endSent)
  }

  def findStartSent(tokens: IndexedSeq[Token], start:Int, end:Int, step:Int) : Int =  {

    for (curPos <- start to end by step) {
       if (tokens(curPos).isStartOfSentence) {
         return curPos
       }
    }

    return end
  }



//  def main(args:Array[String])  {
//    val doc = EntityAnnotationLoader.documentModel("FR940203-0-00059", new File("/usr/aubury/scratch1/jdalton/data/robust04-nlp-annotations-factkb1/FR940203-0-00059.xml"))
//    val result = extractEntityWindowContext(6, doc)
//    result.map(r => println(r._1.mention.entityName + "\t" + r._2._1.map(_.word).mkString(" ")))
//    println()
//
//    val result1 = extractEntityWindowContext(10, doc)
//    result1.map(r => println(r._1.mention.entityName + "\t" + r._2._1.map(_.word).mkString(" ")))
//
//    println()
//
//    val result2 = extractEntityContextSentence(doc)
//    result2.map(r => println(r._1.mention.entityName + "\t" + r._2._1.map(_.word).mkString(" ")))
//
//  }

}
