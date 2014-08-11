package edu.umass.ciir.ede.facc

import java.io.{Writer, StringWriter}
import java.util.concurrent.{ThreadPoolExecutor, ExecutorService, FutureTask}

import com.sun.org.apache.xml.internal.security.utils.I18n
import org.htmlcleaner._

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
                                 text: String, tokenizeText: String => Seq[String], documentName:String="")
  : (Seq[(Option[String], Option[FreebaseEntityAnnotation])], Seq[(FreebaseEntityAnnotation, Int, Int)]) = {

    val textNoExtents =
      text.toLowerCase.indexOf("<mentions>") match {
        case -1 => text
        case idx => text.substring(0, idx)
      }

    // HtmlCleaner has a tendency to go on vacations while parsing some hairy documents, particularly those produced by a certain company starting with M
    if (textNoExtents.substring(500).contains("urn:schemas-microsoft-com:vml")) {
      (Seq(), Seq())
    } else {
      val textClean = cleanHTML(textNoExtents)

      val textSegmentBuilder = new ListBuffer[(Option[String], Option[FreebaseEntityAnnotation])]()
      val annotations2Idx = new ListBuffer[(FreebaseEntityAnnotation, Int, Int)]

      var currBeginIdx = 0

      val annotations2offset = new ListBuffer[(FreebaseEntityAnnotation, Int, Int)]

      for (ann <- faccAnnotations.take(500)) {
        val idx = textClean.indexOf(ann.entityMention, currBeginIdx)
        if (idx == -1) {
          //        println("\n\nText\n"+text+" \n\nannotation \n"+ann+"\n\n all annotations \n"+faccAnnotations)

          System.err.println(getClass.getName + ": " + documentName + " Could not find entity Mention " + ann.entityMention + " in text after offset " + currBeginIdx + ". Skipping...")

          //        throw new RuntimeException(
          //          "Could not find entity Mention " + ann.entityMention + " in text after offset " + currBeginIdx)
        } else {

          val prevText = textClean.substring(currBeginIdx, idx)
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

  val cleaner = {
    val cleaner = new HtmlCleaner()
    // take default cleaner properties
    val props = cleaner.getProperties
    props.setAdvancedXmlEscape(true)
    //    props.setRecognizeUnicodeChars(true)
    //    props.setTransResCharsToNCR(false)
    props.setTranslateSpecialEntities(true)
    props.setCharset("UTF-8")
    props.setOmitComments(true)
    props.setPruneTags("script,style,img")
    props.setOmitHtmlEnvelope(true)
    props.setOmitCdataOutsideScriptAndStyle(true)
    props.setKeepWhitespaceAndCommentsInHead(false)
    props.setOmitDoctypeDeclaration(true)
    props.setOmitXmlDeclaration(true)

    cleaner
  }

  def main(args:Array[String]){
    println( cleanHTML("(this is the case in California, Michigan, and Colorado to name a few). Other states allow patients to go directly to physical therapists. In most cases, if you are not making significant improvement within 30 days, the therapist will refer you to/back to your physician. Seeing a physical therapist first is safe and could save you hundreds of dollars. Click here for details Can my therapist provide me with a diagnosis? In most states, physical therapists cannot make a medical diagnosis. This is something that your medical doctor will provide for you. Physical therapists are important members of your medical team. At this point in time, physicians are typically the health care providers that will provide you with a medical diagnosis. How does the billing process work? Billing for physical therapy services is similar to what happens at your doctor&apos;s office. When you are seen for treatment, the following occurs: The physical therapist bills your insurance company, Workers&apos; Comp, or charges you based on Common Procedure Terminology (CPT) codes. Those codes are transferred to a billing form that is either mailed or electronically communicated to the payer. The payer processes this information and makes payments according to an agreed upon fee schedule. An Explanation of Benefits (EOB) is generated and sent to the patient and the physical therapy clinic with a check for payment and a balance due by the patient. The patient is expected to make the payment on the balance if any. It is important to understand that there are many small steps (beyond the outline provided above) within the process. Exceptions are common to the above example as well. At any time along the way, information may be missing, miscommunicated, or misunderstood. This can delay the payment process. While it is common for the payment process to be completed in 60 days or less, it is not uncommon for the physical therapy clinic to receive payment as long as six months after the treatment date. What will I have to do after physical therapy? Some patients will need to continue with home exercises. Some may choose to continue with a gym exercise program. Others will complete their rehabilitation and return to normal daily activities. It is important that you communicate your goals to your therapist, so he/she can develop a custom program for you. Is my therapist li") )
  }

  def cleanHTML(text:String):String = {

    val node = cleaner.clean(text)
    val writer = new StringWriter()
    new SimpleTextSerializer(cleaner.getProperties).write(node, writer, "UTF-8", true)

    val out = writer.toString
//    val outNoUtf8 = java.text.Normalizer.normalize(out, java.text.Normalizer.Form.NFKD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
    val transliterate =
      out.replaceAll("([A-Z])\\.([A-Z])\\.([A-Z])\\.([A-Z])\\.([A-Z])\\.","$1$2$3$4$5")
        .replaceAll("([A-Z])\\.([A-Z])\\.([A-Z])\\.([A-Z])\\.","$1$2$3$4")
        .replaceAll("([A-Z])\\.([A-Z])\\.([A-Z])\\.","$1$2$3")
        .replaceAll("([A-Z])\\.([A-Z])\\.","$1$2")
        .replaceAllLiterally(65533.toChar+"","'").replaceAllLiterally(8217.toChar+"","'")
        .replaceAllLiterally("&apos;","'")
        .replaceAllLiterally("&quot;","\"")
        .replaceAllLiterally("&gt;",">")
        .replaceAllLiterally("&lt;","<")
        .replaceAllLiterally("&amp;","&")
        //replaceAllLiterally("U.S.","US")
    transliterate.replaceAll("[\n\r]"," ").replaceAll("\\s+"," ")
  }


  /**
   * <p>Simple HTML serializer - creates resulting HTML without indenting and/or compacting.</p>
   */
  class SimpleTextSerializer(props: CleanerProperties) extends HtmlSerializer(props) {

    protected def serialize(tagNode: TagNode, writer: Writer) {
      if (!isMinimizedTagSyntax(tagNode)) {
        import scala.collection.JavaConversions._
        for (item <- tagNode.getAllChildren) {
          item match {
            case _: ContentNode =>
              val content: String = item.toString
              writer.write(Utils.escapeXml(content,  props, false))
//              writer.write(escapeText(content))
            case _ => item match {
              case token: BaseToken =>
                token.serialize(this, writer)
              case _ =>
            }
          }
        }
        writer.write(" ");
      }
    }
  }

}
