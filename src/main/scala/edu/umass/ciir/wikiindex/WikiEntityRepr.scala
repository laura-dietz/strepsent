package edu.umass.ciir.kbbridge.kb2text

import edu.umass.ciir.kbbridge.data.EntityRepr
import edu.umass.ciir.strepsi.galagocompat.GalagoTag
import edu.umass.ciir.strepsi.{SeqTools, TextNormalizer}

import scala.collection.mutable.ListBuffer

/**
 * Build an abstract entity representation from the wikipedia index.
 *
 * In order to have this code be independent of any particular galago version, this class works with the different fields that
 * the galago document class provides. For tags, you need to convert Galago's tag objects to GalagoTag objects.
 * Strepsimur provides a converter function in edu.umass.ciir.strepsimur.galago.compat.CompatConverters
*/
trait EntityReprBuilder {
  def buildEntityRepr(wikipediaTitle:String,  documentName:String, documentText:String, documentTerms:Seq[String], documentTags:Seq[GalagoTag], documentMeta:Map[String,String], passageInfo:Seq[(Int,Int)]):EntityRepr

}

class WikiEntityRepr(val neighborFeatureWeights:Map[String,Double], val buildM:Boolean = true, val termFieldStatistics:(String, String) => Long, val buildNames:Boolean = true, val buildText:Boolean = false) extends EntityReprBuilder{

  def buildEntityRepr(wikipediaTitle:String, documentName:String, documentText:String, documentTerms:Seq[String],documentTags:Seq[GalagoTag], documentMeta:Map[String,String], passageInfo:Seq[(Int,Int)]):EntityRepr = {


    val entityName = WikiTools.wikititleToEntityName(wikipediaTitle)
    val alternativeNameWeightsPerField = WikiNameExtractor.default.getWeightedAnchorNames(entityName, documentTerms, documentTags, documentMeta, termFieldStatistics)

    // ============================
    // alternate names
    val redirect = alternativeNameWeightsPerField("redirect-exact")
    val fbName = alternativeNameWeightsPerField("fbname-exact")
    val anchor = alternativeNameWeightsPerField("anchor-exact")

    val topWeightedNames =
      if(buildNames){

        val weightedNames =
          SeqTools.sumDoubleMaps[String]( Seq(
            multiplyMapValue[String](redirect, 1.0),
            multiplyMapValue[String](fbName, 1.0),
            multiplyMapValue[String](anchor, 0.5)
          ))

        val topWeightedNames = Seq(entityName -> 1.0) ++ SeqTools.topK(weightedNames.toSeq, 10)

        if(topWeightedNames.map(_._2).exists(_.isNaN)){
          println("topWeightedNames contains nan "+topWeightedNames)
          println(redirect)
          println(fbName)
          println(anchor)

        }
        topWeightedNames
      } else Seq.empty


    // ============================
    // neighbors


    val topWeightedNeighbors =
      if(buildM){
      val weightedNeighbors = extractNeighborReprs(entityName, wikipediaTitle, documentName, documentText, documentMeta, passageInfo)
      SeqTools.topK(weightedNeighbors, 10)
    } else Seq.empty


    // ============================
    // word context
//    val stanf_anchor = alternativeNameWeightsPerField("stanf_anchor-exact")
//    val topWords = SeqTools.topK(stanf_anchor.toSeq, 10)
    val topWords =
      if(buildText){
        WikiTextExtractor.extractArticleTermDistribution(10, documentTerms, documentTags)
      } else {
        Seq.empty
      }

    EntityRepr(entityName = entityName, queryId = Some(wikipediaTitle), nameVariants = topWeightedNames, neighbors = topWeightedNeighbors, words = topWords)
  }



  def extractNeighborReprs(entityName:String, wikipediaTitle:String, documentName:String, documentText:String, documentMeta:Map[String,String], passageInfo:Seq[(Int,Int)]): Seq[(EntityRepr, Double)] = {
    val usePassage = passageInfo.nonEmpty
    val passageText =
      if(!usePassage)  ""
      else documentText

    val WikiNeighborExtractor.WikiNeighbors(links, inlinkCount, contextCount) =
      WikiNeighborExtractor.findNeighbors(wikipediaTitle,documentName, documentMeta)
    val destinations = links.groupBy(_.destination)


    case class NeighborScores( paragraphScore:Double, outlinkCount:Int, hasInlink:Boolean, cooccurrenceCount:Int){
      def asFeatureVector:Seq[(String, Double)] =
        Seq(
          "paragraphScore" -> paragraphScore,
          "outlinkCount" -> outlinkCount.toDouble,
          "hasInlink" -> (if(hasInlink) 1.0 else 0.0),
          "cooccurrenceCount" -> cooccurrenceCount.toDouble
        )

      def asNormalizedFeatureVector(normalizer:Seq[(String,Double)]):Seq[(String,Double)] = {
        val normMap = normalizer.toMap
        for((key, value) <- asFeatureVector) yield key -> (value / normMap(key))
      }
    }


    def computeParagraphScore(pId:Int):Double = if(pId < 10) {1.0} else {0.1}
    val neighborinfo =
      (for ((destination, anchors) <- destinations) yield {
        val normDest = WikiTools.wikititleToEntityName(destination)

        val weightedParagraphNeighborSeq = new ListBuffer[(String, Double)]()
        for (anchor <- anchors)  {
          val paragraphScore = computeParagraphScore(anchor.paragraphId)
          val normalizedAnchorText = TextNormalizer.normalizeText(anchor.anchorText)

          if (usePassage){
            if(passageText contains anchor.rawAnchorText){
              weightedParagraphNeighborSeq += normalizedAnchorText -> paragraphScore
            }
          } else {
            weightedParagraphNeighborSeq += normalizedAnchorText -> paragraphScore
          }

        }
        val weightedParagraphNeighbors = SeqTools.groupByAndAggr[String, Double, String, Double](weightedParagraphNeighborSeq, by=TextNormalizer.normalizeText(_), aggr = _.sum)


        val neighborScores = {
          val paragraphScore = weightedParagraphNeighbors.map(_._2).sum
          val outlinkCount = anchors.length
          val hasInlink = inlinkCount.contains(destination)
          val cooccurrenceCount = contextCount(destination)
          NeighborScores(paragraphScore, outlinkCount, hasInlink, cooccurrenceCount)
        }
        ((destination,normDest), weightedParagraphNeighbors, neighborScores)
      }).toSeq

    val summed = SeqTools.sumDoubleMaps(neighborinfo.map(_._3.asFeatureVector.toMap))
    val weightedNeighbors: Seq[(EntityRepr, Double)] =
      for(((dest,normDest), names, neighborScores) <- neighborinfo) yield {
        val normalizedFeature = neighborScores.asNormalizedFeatureVector(summed.toSeq)
        val score = SeqTools.innerProduct(normalizedFeature, neighborFeatureWeights)
        EntityRepr(entityName = normDest, nameVariants = names, wikipediaTitleInput = Some(dest)) -> score
      }

//    val neighborInfo_ = neighborinfo.map(entry => entry._1 -> (entry._2, entry._3)).toMap
//    val weightedNeighbors_ = weightedNeighbors.toMap

    if (weightedNeighbors.exists(_._2.isNaN)){
      println("nans in weightedNeighbors "+weightedNeighbors)
      println("neighborinfo "+neighborinfo)
    }

    weightedNeighbors



  }

  def multiplyMapValue[K](m:Map[K,Double], scalar:Double):Map[K,Double] = {
    for((key,value) <- m) yield key -> (scalar * value)
  }

}




