package edu.umass.ciir.ede.elannotation

import scala.collection.mutable

/**
 * User: dietz
 * Date: 9/20/14
 * Time: 8:31 PM
 */
object PlainTokenConverter {

  case class TokenFragment(charBegin: Int, word: CharSequence) {
    def toToken(charEnd: Int, tokenIdx: Int, isStartOfSentence: Boolean): Token = {
      PlainToken(word.toString, tokenIdx, charBegin, charEnd, isStartOfSentence)
    }

    def appendChar(char: Char): TokenFragment = {
      TokenFragment(charBegin, word + char.toString)
    }

    def peekWord() = word.toString
  }



  def tokenized(text: Seq[String], charBegins:mutable.Buffer[Int],  charEnds:mutable.Buffer[Int], isStop: (String) => Boolean = (_) => false): Seq[Token] = {
    for((term,idx) <- text.zipWithIndex) yield {
      PlainToken(term, idx, charBegins(idx), charEnds(idx), term.head.isUpper)
    }
  }

  def tokenizeFullText(text: String, isStop: (String) => Boolean = (_) => false): Seq[Token] = {
    var isStartOfSentence: Boolean = false
    val charIndexedSeq = text
      .replaceAllLiterally("-", " ")
      .replaceAllLiterally("?", ".")
      .replaceAllLiterally("!", ".")
      .replaceAll("[^a-zA-Z0-9\\.]", " ").zipWithIndex

    val result = charIndexedSeq.foldLeft[(Seq[Token], Option[TokenFragment])](Pair(Seq[Token](), None))
    {
      case (accum: (Seq[Token], Option[TokenFragment]), elem: (Char, Int)) => {
        val list = accum._1
        val frag = accum._2
        val tokenIdx = list.length

        if (frag.isDefined) {
          // we already have a fragment of a token
          elem match {
            case ('.', idx) => {
              // end of sentence! submit token and set flag for the next produced token to have isStartOfSentence true
              if(!isStop(frag.get.peekWord())) {
                val tmp = (list :+ frag.get.toToken(idx, tokenIdx, isStartOfSentence), None)
                isStartOfSentence = true
                tmp
              } else {
                // this is a stopword, dismiss token
                (list, None)
              }
            }
            case (' ', idx) => {
              // new word, submit token (store isStartOfSentence is set)
              if(!isStop(frag.get.peekWord())) {
                val tmp = (list :+ frag.get.toToken(idx, tokenIdx, isStartOfSentence), None)
                isStartOfSentence = false
                tmp
              }else {
                // this is a stopword, dismiss token!
                (list, None)
              }
            }
              // default case, append character
            case (char, idx) => (list, Some(frag.get.appendChar(char)))
          }
        } else {
          // we do not have started a token
          elem match {
            case ('.', idx) => {
              // end of sentence, set flag for next produced token
              isStartOfSentence = true
              (list, None)
            }
            case (' ', idx) => (list, None) // new word, noop
              // start a new token fragment
            case (char, idx) => (list, Some(TokenFragment(idx, char.toString)))
          }
        }
      }
    }

    // handles the case where the string ends mid-token. Append token if it is not a stopword.
    val frag = result._2
    val tokenList =
      if(frag.isDefined && !isStop(frag.get.peekWord())) {
        // submit last token
        result._1 :+ frag.get.toToken(text.length, result._1.length, isStartOfSentence)
      } else result._1

    tokenList
  }
}