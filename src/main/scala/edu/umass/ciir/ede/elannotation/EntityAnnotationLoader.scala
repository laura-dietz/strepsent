package edu.umass.ciir.ede.elannotation

import java.io.File

import edu.umass.ciir.kbbridge.data.{ScoredWikipediaEntity, SimpleEntityMention}

import scala.xml.{Elem, XML}

/**
 * Created with IntelliJ IDEA.
 * User: jdalton
 * Date: 6/18/13
 * Time: 3:49 PM
 */


object EntityAnnotationLoader {


  def entityLinks(docId:String, file:File, candidateLimit:Int = 3) = {
    val xml = XML.loadFile(file)
    xmlToKbLinks(docId, xml, candidateLimit)
  }

  def documentModel(docId:String, file:File, candidateLimit:Int=5) = {
    val xml = XML.loadFile(file)
    val links = xmlToKbLinks(docId, xml, candidateLimit).toIndexedSeq
    val tokens = xmlToTokens(xml).toIndexedSeq
    val mentions = xmlToMentions(xml).toIndexedSeq
    AnnotatedDocument(tokens, mentions, links, None)
  }

  def xmlToKbLinks(docId:String, xmlDoc : Elem, candidateLimit:Int = 300) = {
    val entityLinks = xmlDoc \\ "entitylink"
    for (e <- entityLinks) yield {
      //  println((e \ "name").text)
      val mention = new SimpleEntityMention(docId, "", (e \ "mentionId").text.trim, (e \ "name").text.trim, "")
      val charBegin = (e \ "CharacterOffsetBegin").text.trim.toInt
      val charEnd = (e \ "CharacterOffsetEnd").text.trim.toInt
      val tokenBegin = (e \ "TokenBegin").text.trim.toInt
      val tokenEnd = (e \ "TokenEnd").text.trim.toInt

      val candidates = e \\ "candidate"  take candidateLimit
      val candidateEntities = for (c <- candidates) yield {
        new ScoredWikipediaEntity( (c \ "id").text.trim, -1, (c \ "score").text.trim.toDouble, (c \ "rank").text.trim.toInt)
      }
      val linkedMention = LinkedMention(mention, candidateEntities, charBegin, charEnd, tokenBegin, tokenEnd)
      // println(linkedMention)
      linkedMention
    }
  }

  def xmlToTokens(xmlDoc : Elem) = {
    val tokens = xmlDoc \\ "token"
    for (t <- tokens) yield {
      //  println((e \ "name").text)
      val id = t.attributes.asAttrMap("id").toInt
      val word = (t \ "word").text.trim
      val charBegin = (t \ "CharacterOffsetBegin").text.trim.toInt
      val charEnd = (t \ "CharacterOffsetEnd").text.trim.toInt
      val pos = (t \ "POS").text.trim
      val ner =  (t \ "NER").text.trim
      val parse =  (t \ "PARSE").text.trim
      val sentenceStart = (t \ "StartSentence").text.trim.toBoolean

      val token = NlpToken(word, id, pos, ner, parse, charBegin, charEnd, sentenceStart )
      // println(linkedMention)
      token
    }
  }

  def xmlToMentions(xmlDoc : Elem) = {
    val mentions = xmlDoc \\ "mention"
    for (m <- mentions) yield {
      val string = (m \ "string").text.trim
      val charBegin = (m \ "CharacterOffsetBegin").text.trim.toInt
      val charEnd = (m \ "CharacterOffsetEnd").text.trim.toInt
      val mentionType = (m \ "type").text.trim
      val tokenBegin = (m \ "TokenBegin").text.trim.toInt
      val tokenEnd = (m \ "TokenEnd").text.trim.toInt

      val token = Mention(string, mentionType, charBegin, charEnd, tokenBegin:Int, tokenEnd:Int)
      // println(linkedMention)
      token
    }
  }

  def main(args: Array[String]) {

    entityLinks("FBIS3-10082", new File("/usr/aubury/scratch1/jdalton/data/robust04-nlp-annotations-factkb1/FBIS3-10082.xml"))
  }

}
