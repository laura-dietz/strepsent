package edu.umass.ciir.ede.elannotation

import edu.umass.ciir.strepsi.StopWordList

/**
 * User: dietz
 * Date: 9/20/14
 * Time: 8:26 PM
 */
class ConversionTest extends junit.framework.TestCase {
  def testConverter(): Unit = {
    val text = "Laura is the best person. In the great wide world"
    val tokens = PlainTokenConverter.tokenizeFullText(text, StopWordList.isStopWord)

    for(tok <- tokens){
      assert(tok.word == text.substring(tok.charBegin, tok.charEnd), "charoffsets dont match "+tok+" substring = "+text.substring(tok.charBegin, tok.charEnd))

    }

    assert(tokens.exists(_.isStartOfSentence), "missing start-of-sentence marker")
    println(tokens.mkString("\n"))
  }

}
