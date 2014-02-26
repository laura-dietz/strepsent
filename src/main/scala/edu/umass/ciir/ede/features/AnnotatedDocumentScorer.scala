package edu.umass.ciir.ede.features

import scala.collection.JavaConversions._
import edu.umass.ciir.ede.elannotation.AnnotatedDocument
import edu.umass.ciir.strepsi.{ScoredDocument}
import edu.umass.ciir.strepsi.ciirshared.LanguageModel
import edu.umass.ciir.strepsi.termcounts.TermCollectionCountsMap


/**
 * Created by jdalton on 1/22/14.
 */
class AnnotatedDocumentScorer(wikipediaCategoryCountsPath:String, wikipediaTypeCountsPath:String) {

  val (categoryCollFreq, categoryCounts) = TermCollectionCountsMap.loadMap(wikipediaCategoryCountsPath)
  val (typeCollFreq, typeCounts) = TermCollectionCountsMap.loadMap(wikipediaTypeCountsPath)
//  val (categoryCollFreq, categoryCounts) = TermCountsMap.loadMap(EdeConfig.wikipediaCategoryCounts)
//  val (typeCollFreq, typeCounts) = TermCountsMap.loadMap(EdeConfig.wikipediaTypeCounts)


  def scoreDocumentQlIdentifiers(queryIdentifiers: Seq[(String, Double)], annotations : Map[String, AnnotatedDocument],
                                 workingSet: Set[String], nilThreshold: Double = 0.5,
                                 collectionIdCount:Long, entityIdCounts: Map[String, (Long, Long)]) : Seq[ScoredDocument] = {

    val bgScore = computeBgScore(queryIdentifiers, collectionIdCount,entityIdCounts)
    val scoredDocs = for (doc <- workingSet) yield {

      val annotationOption = annotations.get(doc)
      val score = annotationOption match {
        case Some(ann) => {
          val links = ann.kbLinks.map(l => l.entityLinks.filter(_.score > nilThreshold)).flatten
          val linkIds = links.map(_.wikipediaTitle)
          scoreQl(queryIdentifiers, linkIds, 1500, collectionIdCount,entityIdCounts)
        }
        case None => bgScore
      }

      new ScoredDocument(doc, -1, score)
    }
    val reranked = scoredDocs.toSeq.sortBy(d => -d.score)
    val rerankedWithNewRank =
    for ((result, idx) <- reranked.zipWithIndex) yield {
      result.withNewRank(idx + 1)
      //println(result.rank + " " + result.documentName + " " + result.score)

    }
    rerankedWithNewRank.toSeq
  }


  def scoreDocumentCategoriesIdentifiers(queryIdentifiers: Seq[(String, Double)], annotations : Map[String, AnnotatedDocument],
                                         workingSet: Set[String], nilThreshold: Double = 0.5,
                                         typeMap: Map[String, (Set[String], Set[String])],
                                          typesOrCategories:String, mu:Int=100) : Seq[ScoredDocument] = {


    val bgScore = if (typesOrCategories equals "types") {
      computeBgScore(queryIdentifiers, typeCollFreq, typeCounts, mu)
    } else {
      computeBgScore(queryIdentifiers, categoryCollFreq, categoryCounts, mu)
    }

    val scoredDocs = for (doc <- workingSet) yield {

      val annotationOption = annotations.get(doc)
      val score = annotationOption match {
        case Some(ann) => {
          val (categories, types) = CategoryFeatureExtractor.extractCategoriesAndTypes(ann, 0.5, typeMap)

          val qlScore =  if (typesOrCategories equals "types") {
            scoreQl(queryIdentifiers, types, mu, typeCollFreq, typeCounts)
          } else {
            scoreQl(queryIdentifiers, categories, mu, categoryCollFreq, categoryCounts)
          }
          qlScore
        }
        case None => bgScore
      }
      new ScoredDocument(doc, -1, score)
    }
    val reranked = scoredDocs.toSeq.sortBy(d => -d.score)
    val rerankedWithNewRank = 
    for ((result, idx) <- reranked.zipWithIndex) yield {
      result.withNewRank(idx + 1)
      //println(result.rank + " " + result.documentName + " " + result.score)

    }
    rerankedWithNewRank.toSeq
  }



  def computeBgScore(queryIdentifiers: Seq[(String, Double)], collLength: Long, collectionCountMap: (String) =>
                    (Long, Long), mu:Int=1500) : Double = {

    val termScores = for ((q, weight) <- queryIdentifiers) yield {
      val collFreq = collectionCountMap(q) match {
        case (0L,_ ) => 1L
        case (x, _) => x
      }

      val cf = if (collFreq == 0) {
        0.5 / collLength
      } else {
        collFreq.toDouble / collLength
      }

      val c = 0
      val l = 2000 // average doc len is around 60
      val prob = DocumentScoringUtilities.dirichletSmoothedProb(l, c, mu, cf)

      val termScore = weight * prob
      termScore
    }
    return termScores.sum
  }

  def scoreQl(queryIdentifiers: Seq[(String, Double)], parsedTokens:Seq[String], mu:Int=1500, collLength:Long,
              collectionCountMap: (String) => (Long, Long)) : Double = {

    val fieldLm = new LanguageModel(1)
    fieldLm.addDocument(parsedTokens, false)
    fieldLm.calculateProbabilities()

    val termScores = for ((q, weight) <- queryIdentifiers) yield {
      val collFreq = collectionCountMap(q)._1

      val cf = if (collFreq == 0) {
        0.5 / collLength
      } else {
        collFreq.toDouble / collLength
      }

      val te = fieldLm.getTermEntry(q)
      val c = if (te != null) {
        te.getFrequency
      } else {
        0
      }
      val l = fieldLm.getCollectionFrequency

      val prob = DocumentScoringUtilities.dirichletSmoothedProb(l, c, mu, cf)


      val termScore = weight * prob
      termScore
    }
    return termScores.sum

  }

}
