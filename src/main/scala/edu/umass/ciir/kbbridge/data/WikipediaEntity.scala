package edu.umass.ciir.kbbridge.data


/**
 * User: jdalton
 * Date: 3/29/13
 */
case class WikipediaEntity (wikipediaTitle:String, wikipediaId:Int, var incomingLinks : Set[String] = Set(), var outgoingLinks : Set[String] = Set(), var combinedLinks : Set[String] = Set())   {
  def name:String = wikipediaTitle.replaceAll("_", " ")
}


