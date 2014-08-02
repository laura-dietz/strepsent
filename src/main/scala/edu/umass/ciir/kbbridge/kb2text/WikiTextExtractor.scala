package edu.umass.ciir.kbbridge.kb2text

import edu.umass.ciir.strepsi.distribution.Distribution
import edu.umass.ciir.strepsi.{SeqTools, StopWordList}
import edu.umass.ciir.strepsi.galagocompat.GalagoTag

/**
 * Get the wikipedia article's text from the galago document by parsing the content of the &lt;text&gt; extent.
 */
object WikiTextExtractor {
  def extractArticleText(documentTerms:Seq[String],documentTags:Seq[GalagoTag]):Seq[String] = {
    val textPerTag =
      for(tag <- documentTags; if tag.name =="text") yield {
        documentTerms.slice(tag.begin, tag.end).filterNot(StopWordList.isStopWord)
      }
    textPerTag.flatten
  }

  def extractArticleTermDistribution(numTopTerms:Int, documentTerms:Seq[String],documentTags:Seq[GalagoTag]): Seq[(String,Double)] = {
    val textTerms = extractArticleText(documentTerms, documentTags)
    val termCounts = SeqTools.countMap[String](textTerms)
    if (termCounts.nonEmpty) {
      Distribution[String](SeqTools.mapValuesToDouble(termCounts).toSeq).topK(numTopTerms).normalize.distr
    } else Seq.empty

  }
}
