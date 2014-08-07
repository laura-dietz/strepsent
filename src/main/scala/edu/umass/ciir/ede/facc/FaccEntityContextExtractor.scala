package edu.umass.ciir.ede.facc

import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer

/**
 * Extracts term and neighbor context of FreebaseEntityAnnotations.
 */
object FaccEntityContextExtractor {
  val DEBUG = false

  /*
   * Like ContextExtractor in elannotation package
   *
   */
  def extractWindowContext(text:String, termWindowSize: Int
                           , metadata:Map[String, String]
                           , documentName:String
                           , tokenizeText:(String) => Seq[String]): Seq[(FreebaseEntityAnnotation,  Seq[String], Seq[FreebaseEntityAnnotation])] = {
    val halfWindowSize = Math.rint(termWindowSize / 2).toInt

    val faccAnnotations = FaccAnnotationsFromDocument.extractFaccAnnotations(metadata,documentName)



      /* Splits the raw text according to surface forms of annotations (in order of annotations)
        * Uses TagTokenizer to tokenize (and filter) non-entity text segments
        * Uses TagTokenizer to tokenize mention and attach annotation to the token (for multi-token annotations,
        * we only attach it to the first token
        *
        * This way we build a token-by-token sequence with elements (Option[Token],
        * Option[Annotation]) in textSegmentBuilder
        * We keep track of token-indexes (begin and end) that are annotated with entities in annotations2Idx
      */

      println(" processing annotations in document "+documentName)
      val (textSegments, annotations2Idx )= segmentTextWithAnnotations(faccAnnotations, text, tokenizeText)
  
      /* In a second pass, we iterate over these annotated token-indexes, get tokens from half a window before and after
      * We unzip the  (Option[Token], Option[Annotation]) sequence to get a list of terms and neighbor annotations.
        */
      val result =
        for ((ann, tokenBegin, tokenEnd) <- annotations2Idx) yield {
          val segmentWindow = textSegments.slice(tokenBegin - halfWindowSize, tokenEnd + halfWindowSize)
          val terms = segmentWindow.map(_._1).flatten
          val neighbors = segmentWindow.map(_._2).flatten.filterNot(_.freebaseId == ann.freebaseId)
          (ann, terms, neighbors)
        }
  
  
      /*
       * Output for debugging
       */
      if (DEBUG) {
        for ((ann, terms, neighbors) <- result) {
          println(ann.entityMention + " " + ann.freebaseId + "\n  " + terms.mkString(" ") +
            neighbors.map(_.entityMention).mkString("\n     ", "\n     ", ""))
        }
      }
  
      result
  }


  /** Splits the raw text according to surface forms of annotations (in order of annotations)
    * Uses TagTokenizer to tokenize (and filter) non-entity text segments
    * Uses TagTokenizer to tokenize mention and attach annotation to the token (for multi-token annotations,
    * we only attach it to the first token
    *
    * This way we build a token-by-token sequence with elements (Option[Token],
    * Option[Annotation]) in textSegmentBuilder
    * We keep track of token-indexes (begin and end) that are annotated with entities in annotations2Idx
    *
    * @param faccAnnotations sequence of annotations, in order which they appear in the text
    * @param text            raw text, will be processed by TagTokenizer
    * @return a pair of a)
    *         Sequence over tokens, for every token a pair of Option[Term],
    *         Option[Annotation]
    *         b) a list of token ids (for a) where annotations occur.
    *
    */
  def segmentTextWithAnnotations(faccAnnotations: Seq[FreebaseEntityAnnotation],
                                 text: String, tokenizeText: String => Seq[String])
  : (Seq[(Option[String], Option[FreebaseEntityAnnotation])], Seq[(FreebaseEntityAnnotation, Int, Int)]) = {

    val textSegmentBuilder = new ListBuffer[(Option[String], Option[FreebaseEntityAnnotation])]()
    val annotations2Idx = new ListBuffer[(FreebaseEntityAnnotation, Int, Int)]

    var currBeginIdx = 0

    for (ann <- faccAnnotations) {
      val idx = text.indexOf(ann.entityMention, currBeginIdx)
      if (idx == -1) {
        println("\n\nText\n"+text+" \n\nannotation \n"+ann+"\n\n all annotations \n"+faccAnnotations)


        throw new RuntimeException(
          "Could not find entity Mention " + ann.entityMention + " in text after offset " + currBeginIdx)
      }

      val prevText = text.substring(currBeginIdx, idx)
      textSegmentBuilder ++= tokenizeText(prevText).map(t => (Some(t), None))
      val tokenBegin = textSegmentBuilder.length

      // for multi-term mentions, annotations only get attached to the first term
      textSegmentBuilder ++= tokenizeText(ann.entityMention).map(Some(_))
        .zipAll(Seq(Some(ann)), None, None)

      val tokenEnd = textSegmentBuilder.length
      annotations2Idx += Tuple3(ann, tokenBegin, tokenEnd)

      currBeginIdx = idx + ann.entityMention.length
    }

    (textSegmentBuilder.toSeq, annotations2Idx.toSeq)
  }

}
