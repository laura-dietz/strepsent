package edu.umass.ciir.ede.facc

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
                                    freebaseId:String) {

  def wikipediaTitle = Freebase2WikipediaMap.freebaseId2WikiTitleMap(freebaseId)

}

case class FreebaseEntityQueryAnnotation(topicId:Int,
                                    topicDescription:String,
                                    entityMention:String,
                                    beginByteOffset:Int,
                                    endByteOffset:Int,
                                    freebaseId:String,
                                    posterior:Double) {
  def wikipediaTitle = Freebase2WikipediaMap.freebaseId2WikiTitleMap(freebaseId)
}