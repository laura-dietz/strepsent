package edu.umass.ciir.kbbridge.data

/**
 * User: dietz
 * Date: 2/25/14
 * Time: 2:53 PM
 */
class ScoredWikipediaEntity(override val wikipediaTitle:String,
                            override val wikipediaId:Int,
                            var score:Double,
                            var rank:Int,
                            val featureMap:Option[Map[String,Double]]=None)
  extends WikipediaEntity (wikipediaTitle,wikipediaId)
