package edu.umass.ciir.wikiindex

import edu.umass.ciir.strepsi.galagocompat.GalagoTag
import edu.umass.ciir.strepsi.{SeqTools, TextNormalizer}

import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.collection.mutable.ListBuffer


/**
 * Extracts alternative names from wikipedia stored in the extents anchor-exact, redirect-exact, fbname-exact and
 * optionally stanf_anchor-extract (only for "allNameFields)
 */
object WikiNameExtractor {
  val reliableNameFields = Set("anchor-exact", "redirect-exact", "fbname-exact")
  val allNameFields = Set("stanf_anchor-exact", "anchor-exact", "redirect-exact", "fbname-exact")
  val default = new WikiNameExtractor(reliableNameFields)

  def getAnchorNameCounts(entityName: String, anchorField: String, documentTerms:Seq[String], documentTags:Seq[GalagoTag],documentMeta:Map[String,String]): Map[String, Int] = {
    val fields = documentTags
    val terms = documentTerms

    val tokenMap = ListBuffer[String]()
    val normalizedQuery = TextNormalizer.normalizeText(entityName)

    for (f <- fields.filter(_.name == anchorField)) {
      val fieldValue = terms.subList(f.begin, f.end).mkString(" ").trim()
      tokenMap += fieldValue
    }

    val alternativeNames = SeqTools.countMap(tokenMap)

    val filtered = alternativeNames.filterKeys(alternateName => {
      val normAlternateName = TextNormalizer.normalizeText(alternateName)
      !(normalizedQuery contains normAlternateName) &&
        ! (normalizedQuery equals normAlternateName) &&
        ! (normAlternateName.length == 0)
    })
    filtered
  }

  def getAnchorProbs(termCounts: Map[String, Int], anchorField: String, getTermFieldStatistic:(String, String) => Long): Map[String, Double] = {

    val anchorProbs = new mutable.HashMap[String, Double]()
    for ((alternateName, inlinkCount) <- termCounts){
      val totalAnchorCount = getTermFieldStatistic(alternateName, anchorField) + 0.5

      if (totalAnchorCount <= inlinkCount) {
        anchorProbs += (alternateName -> 1.0)
      } else {
        val stanfLinkProb = 1.0 * inlinkCount / totalAnchorCount
        anchorProbs += (alternateName -> stanfLinkProb)
      }
    }
    anchorProbs.toMap
  }

  def getWeightedAnchorNames(entityName:String, documentTerms:Seq[String],documentTags:Seq[GalagoTag], documentMeta:Map[String,String], getTermFieldStatistics:(String, String) => Long, fieldsToCount:Set[String]):Map[String, Map[String,Double]] = {
    val result = new ListBuffer[(String, Map[String,Double])]()
    for(anchorField <- fieldsToCount)  {
      val nameCounts = getAnchorNameCounts(entityName, anchorField, documentTerms, documentTags, documentMeta)
      val nameWeights = getAnchorProbs(nameCounts, anchorField, getTermFieldStatistics)
      result += (anchorField -> nameWeights)
    }
    result.toMap
  }


}

/**
 * Extracts alternative names from wikipedia stored in the given name extents.
 */
class WikiNameExtractor(val nameFieldsToCount:Set[String]) {
  import WikiNameExtractor._

  def getAlternativeNames(entityName: String, documentTerms:Seq[String], documentTags:Seq[GalagoTag]): Seq[String] = {
    val fields = documentTags
    val terms = documentTerms

    //    val fieldsToCount = Set("stanf_anchor-exact", "anchor-exact", "redirect-exact", "fbname-exact")
    var fieldExactMatchCount = 0
    val tokenMap = scala.collection.mutable.HashMap[String, ListBuffer[String]]()


    val normalizedQuery = TextNormalizer.normalizeText(entityName)

    for (f <- fields) {
      if (nameFieldsToCount contains f.name) {
        val fieldValue = terms.subList(f.begin, f.end).mkString("").trim()
        val curTokenList = tokenMap.getOrElseUpdate(f.name, ListBuffer[String]()) += fieldValue
        tokenMap.update(f.name, curTokenList)
      }
    }

    val alternativeNames = tokenMap.getOrElse("anchor-exact", Seq()) ++ tokenMap.getOrElse("redirect-exact", Seq()) ++ tokenMap.getOrElse("fbname-exact", Seq())
    val filtered = alternativeNames.filter(alternateName => !(normalizedQuery contains TextNormalizer.normalizeText(alternateName)) && !(normalizedQuery equals TextNormalizer.normalizeText(alternateName)) && !(TextNormalizer.normalizeText(alternateName).length == 0))
    filtered.toSet.toSeq
  }


  def getWeightedAnchorNames(entityName:String, documentTerms:Seq[String],documentTags:Seq[GalagoTag], documentMeta:Map[String,String], getTermFieldStatistics:(String, String) => Long):Map[String, Map[String,Double]] = {
    WikiNameExtractor.getWeightedAnchorNames(entityName, documentTerms,documentTags, documentMeta, getTermFieldStatistics, nameFieldsToCount)
  }


}