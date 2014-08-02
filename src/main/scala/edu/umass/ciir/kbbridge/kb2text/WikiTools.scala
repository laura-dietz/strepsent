package edu.umass.ciir.kbbridge.kb2text

import edu.umass.ciir.strepsi.StringTools

/**
 * User: dietz
 * Date: 8/1/14
 * Time: 8:19 PM
 */
object WikiTools {
  def wikititleToEntityName(wikititle:String):String = {
    StringTools.zapParentheses(wikititle.replaceAllLiterally("_"," "))
  }


}
