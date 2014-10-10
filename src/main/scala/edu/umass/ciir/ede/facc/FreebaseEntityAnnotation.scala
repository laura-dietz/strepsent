package edu.umass.ciir.ede.facc

import edu.umass.ciir.strepsent.{WikiEntityId, FreebaseEntityId}

/**
 * Created with IntelliJ IDEA.
 * User: jdalton
 * Date: 8/22/13
 * Time: 2:58 PM
 */

case class FreebaseDocumentAnnotations(documentName:String, annotations:Seq[FreebaseDocumentAnnotations])

case class FreebaseEntityAnnotation(documentName:String,
                                    encoding:String,
                                    entityMention:String,
                                    beginByteOffset:Int,
                                    endByteOffset:Int,
                                    mentionContextPosterior:Double,
                                    contextOnlyPosterior:Double,
                                    freebaseId:FreebaseEntityId) {

  def wikipediaTitle:WikiEntityId = Freebase2WikipediaMap.freebaseId2WikiTitleMap.getOrElse(freebaseId,"")

}

case class FreebaseEntityQueryAnnotation(topicId:Int,
                                    topicDescription:String,
                                    entityMention:String,
                                    beginByteOffset:Int,
                                    endByteOffset:Int,
                                    freebaseId:FreebaseEntityId,
                                    posterior:Double) {
  def wikipediaTitle:WikiEntityId = Freebase2WikipediaMap.freebaseId2WikiTitleMap.getOrElse(freebaseId,"")
}