package edu.umass.ciir.ede.facc

import edu.umass.ciir.ede.elannotation.AnnotatedDocument
import edu.umass.ciir.strepsi.SeqTools
import edu.umass.ciir.strepsitag.FastNameTagger

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/**
 * Extracts term and neighbor context of FreebaseEntityAnnotations.
 */
object FaccEntityContextExtractor {
  val DEBUG = true // todo switch DEBUG off

  /*
   * Like ContextExtractor in elannotation package
   *
   */
  def extractWindowContext(text:String, termWindowSize: Int
                           , metadata:Map[String, String]
                           , documentName:String
                           , tokenizeText:(String) => Seq[String]
                           , annotationMap:Map[String, AnnotatedDocument]): Seq[(FreebaseEntityAnnotation,  Seq[String], Seq[FreebaseEntityAnnotation])] = {
    val halfWindowSize = Math.rint(termWindowSize / 2).toInt

    //    val faccAnnotations = FaccAnnotationsFromDocument.extractFaccAnnotations(metadata,documentName)
    val faccAnnotations = annotationMap(documentName).faccAnnotations.getOrElse(Seq())

    /* Splits the raw text according to surface forms of annotations (in order of annotations)
      * Uses TagTokenizer to tokenize (and filter) non-entity text segments
      * Uses TagTokenizer to tokenize mention and attach annotation to the token (for multi-token annotations,
      * we only attach it to the first token
      *
      * This way we build a token-by-token sequence with elements (Option[Token],
      * Option[Annotation]) in textSegmentBuilder
      * We keep track of token-indexes (begin and end) that are annotated with entities in annotations2Idx
    */

    val (textSegments, annotations2Idx )= segmentTextWithAnnotations(faccAnnotations, text, tokenizeText, documentName)

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


  /** uses name-tagger to split the raw text according to surface forms of annotations (in order of annotations)
    * Uses TagTokenizer to tokenize (and filter) non-entity text segments
    * Uses TagTokenizer to tokenize mention and attach annotation to the token (for multi-token annotations,
    * we only attach it to the first token
    *
    * This way we build a token-by-token sequence with elements (Option[Token],
    * Option[Annotation]) in textSegmentBuilder
    * We keep track of token-indexes (begin and end) that are annotated with entities in annotations2Idx
    *
    * @param faccAnnotations sequence of annotations, in order which they appear in the text
    * @param text            clean text
    * @return a pair of a)
    *         Sequence over tokens, for every token a pair of Option[Term],
    *         Option[Annotation]
    *         b) a list of token ids (for a) where annotations occur.
    *
    */
  def segmentTextWithAnnotations(faccAnnotations: Seq[FreebaseEntityAnnotation],
                                 text: String, tokenizeText: String => Seq[String], documentName:String="")
  : (Seq[(Option[String], Option[FreebaseEntityAnnotation])], Seq[(FreebaseEntityAnnotation, Int, Int)]) = {

    val textSegmentBuilder = new ListBuffer[(Option[String], Option[FreebaseEntityAnnotation])]()
    val annotations2Idx = new ListBuffer[(FreebaseEntityAnnotation, Int, Int)]
    val annotations2offset = new ListBuffer[(FreebaseEntityAnnotation, Int, Int)]

    var currBeginIdx = 0


    def normalizeName(name:String):String = {
      name.replaceAll("\\s+", "").replaceAll("[^a-zA-Z0-9]", "")
    }


    val faccByNormalizedNames = faccAnnotations.map(x => normalizeName(x.entityMention) -> x)
    val faccByNameId = SeqTools.mapValues[String, Iterable[FreebaseEntityAnnotation], mutable.Buffer[FreebaseEntityAnnotation]](
      SeqTools.groupByKey(faccByNormalizedNames.sortBy(_._2.beginByteOffset)),
      _.toBuffer)

    val nameDict = faccByNormalizedNames.map(x => x._2.entityMention -> x._1)
    val tempFile = FastNameTagger.createTmpDictionary(nameDict, "faccAnnotations")
    val nametagger = new FastNameTagger(tempFile, true, true)
    val nameMatches = nametagger.tag(text).sortBy(_.lower)
    val nonOverlapping = FastNameTagger.removeOverlappingMatches(nameMatches)

    println("nameDict = "+nameDict)
    println("nameMatches = "+nameMatches)

    for (m <- nonOverlapping; if faccByNameId(m.nameId).nonEmpty) {

      val prevText = text.substring(currBeginIdx, m.lower)
      textSegmentBuilder ++= tokenizeText(prevText).map(t => (Some(t), None))
      val tokenBegin = textSegmentBuilder.length

      val ann = faccByNameId(m.nameId).remove(0)
      // for multi-term mentions, annotations only get attached to the first term
      textSegmentBuilder ++= tokenizeText(m.mention).map(Some(_))
        .zipAll(Seq(Some(ann)), None, None)

      val tokenEnd = textSegmentBuilder.length
      annotations2Idx += Tuple3(ann, tokenBegin, tokenEnd)
      annotations2offset += Tuple3(ann, m.lower, m.upper)

      currBeginIdx = m.upper
    }
    println("textSegmentBuilder = "+ textSegmentBuilder)

    tempFile.delete()
    (textSegmentBuilder.toSeq, annotations2Idx.toSeq)
  }


  /** Old version that uses iterative substring finding - Splits the raw text according to surface forms of annotations (in order of annotations)
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
  def segmentTextWithAnnotationsOld(faccAnnotations: Seq[FreebaseEntityAnnotation],
                                    text: String, tokenizeText: String => Seq[String], documentName:String="")
  : (Seq[(Option[String], Option[FreebaseEntityAnnotation])], Seq[(FreebaseEntityAnnotation, Int, Int)]) = {
    //
    //    val textNoExtents =
    //      text.toLowerCase.indexOf("<mentions>") match {
    //        case -1 => text
    //        case idx => text.substring(0, idx)
    //      }
    //
    //    // HtmlCleaner has a tendency to go on vacations while parsing some hairy documents, particularly those produced by a certain company starting with M
    //    if (textNoExtents.substring(0,500).contains("urn:schemas-microsoft-com:vml")) {
    //      (Seq(), Seq())
    //    } else {
    //      val textgclean = tokenizeText(textNoExtents).mkString(" ")
    //      val textClean = cleanHTML(textgclean)
    //      //val textClean = cleanHTML(textNoExtents)

    val textSegmentBuilder = new ListBuffer[(Option[String], Option[FreebaseEntityAnnotation])]()
    val annotations2Idx = new ListBuffer[(FreebaseEntityAnnotation, Int, Int)]

    var currBeginIdx = 0

    val annotations2offset = new ListBuffer[(FreebaseEntityAnnotation, Int, Int)]

    // todo replace with name-tagger!
    // todo snip!
    val textCleanLower = text.toLowerCase
    for (ann <- faccAnnotations.take(500)) {
      val idx = textCleanLower.indexOf(tokenizeText(ann.entityMention.toLowerCase).mkString(" "), currBeginIdx)

      if (idx == -1) {
        //        println("\n\nText\n"+text+" \n\nannotation \n"+ann+"\n\n all annotations \n"+faccAnnotations)

        System.err.println(getClass.getName + ": " + documentName + " Could not find entity Mention " + ann.entityMention + " in text after offset " + currBeginIdx + ". Skipping...")


      } else {

        // todo snap!

        val prevText = text.substring(currBeginIdx, idx)
        textSegmentBuilder ++= tokenizeText(prevText).map(t => (Some(t), None))
        val tokenBegin = textSegmentBuilder.length

        // for multi-term mentions, annotations only get attached to the first term
        textSegmentBuilder ++= tokenizeText(ann.entityMention).map(Some(_))
          .zipAll(Seq(Some(ann)), None, None)

        val tokenEnd = textSegmentBuilder.length
        annotations2Idx += Tuple3(ann, tokenBegin, tokenEnd)
        annotations2offset += Tuple3(ann, idx, idx + ann.entityMention.length)

        currBeginIdx = idx + ann.entityMention.length
      }
    }

    (textSegmentBuilder.toSeq, annotations2Idx.toSeq)
  }


}
