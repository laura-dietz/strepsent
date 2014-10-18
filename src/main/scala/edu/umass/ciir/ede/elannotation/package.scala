package edu.umass.ciir.ede

import edu.umass.ciir.ede.facc.FreebaseEntityAnnotation
import edu.umass.ciir.kbbridge.data.{EntityMention, ScoredWikipediaEntity}
import edu.umass.ciir.strepsi.trec.Judgment

/**
 * Created with IntelliJ IDEA.
 * User: jdalton
 * Date: 8/27/13
 * Time: 6:04 PM
 * To change this template use File | Settings | File Templates.
 */
package object elannotation {

  type QueryId = String
  type ElEntityId = String
  type QueryJudgmentSet = Map[QueryId, Seq[Judgment]]

  type WikiTitle = String
  type DocumentName = String

  case class LinkedMention(mention: EntityMention, entityLinks: Seq[ScoredWikipediaEntity], charBegin:Int, charEnd:Int, tokenBegin:Int, tokenEnd:Int)

  trait Token{def word:String; def position:Int; def charBegin:Int; def charEnd:Int; def isStartOfSentence:Boolean}

  case class PlainToken(word:String, position:Int, charBegin:Int, charEnd:Int,isStartOfSentence:Boolean) extends Token
  case class NlpToken(word:String, position:Int, posType:String, nerType:String, parse:String, charBegin:Int, charEnd:Int, isStartOfSentence:Boolean) extends Token

  case class Mention(string:String, mentionType:String, charBegin:Int, charEnd:Int, tokenBegin:Int, tokenEnd:Int)

  case class AnnotatedDocument(tokens: IndexedSeq[Token], mentions:IndexedSeq[Mention], kbLinks: IndexedSeq[LinkedMention], faccAnnotations : Option[Seq[FreebaseEntityAnnotation]])


}
