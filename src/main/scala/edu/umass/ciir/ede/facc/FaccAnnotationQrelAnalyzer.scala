//package edu.umass.ciir.ede.facc
//
//import java.io.{PrintWriter, File}
//import edu.umass.ciir.ede.EdeConfig
//import edu.umass.ciir.galago.GalagoSearcher
//import org.lemurproject.galago.core.eval.QuerySetJudgments
//import scala.collection.JavaConversions._
//import scala.collection.mutable.ListBuffer
//
///**
// * Created by jdalton on 1/2/14.
// */
//object FaccAnnotationQrelAnalyzer extends App {
//
//
//  val queryFile = new File(EdeConfig.clueRevisedAnnotatedQueryFile)
//  val qrelPath = (EdeConfig.clueBDocumentQrels)
//  val qrels = new QuerySetJudgments(qrelPath, true, true)
//
//  println("Loading queries: " + queryFile + " qrels: " + qrelPath)
//
//  val queries = FreeebaseQueryAnnotationReader.loadRevisedAnnotations(queryFile) //.filter(q => Set("C09-171").contains(q._1))
//
//  val params = EdeConfig.cluebFacc
//
//  val searcher = GalagoSearcher(EdeConfig.clueweb09BCollectionPath, params)
//
//  var totalDocs = 0
//  var totalMissing = 0
//  var totalWikiMissing = 0
//  var totalWiki = 0
//  var totalWithEntity = 0
//
//  val allQueryKbLinks = ListBuffer[(Int,Seq[AnnotationStat])]()
//
//  val batchResults =
//    for (queryId <- 1 to 200) yield {
//     // println("queryId = " + queryId)
//
//      val docAnnotationStats = ListBuffer[Seq[AnnotationStat]]()
//
//      val queryJudgments = qrels.get("C09-"+queryId)
//      var numMissingDocs = 0
//      var docsWithEntity = 0
//      var numRel = 0
//      var missingWiki = 0
//
//      if (queryJudgments == null) {
//        println("no judgments for query.")
//      } else {
//
//        val docs = queryJudgments.getDocumentSet
//        val filteredDocs = docs.filter(docId => {
//          val components = docId.split("-")
//          val chunk = components(1).replace("en", "")
//          if (chunk.contains("wp")) {
//            true
//          } else {
//            if (chunk.toInt < 12) {
//              true
//            } else {
//              false
//            }
//          }
//        })
//
//    //    println("num judgments: " + queryJudgments.size() + " clueb: " + filteredDocs.size + " pos: " + queryJudgments.getRelevantJudgmentCount + " negative:" + queryJudgments.getNonRelevantJudgmentCount)
//
//        for (docId <- filteredDocs) {
//
//          // only rel docs
//          if (queryJudgments.get(docId) > 0) {
//            val doc = searcher.pullDocument(docId)
////            if (docId.contains("enwp")) {
////              numWiki += 1
////            }
//
//          //  println(docId + "\t" + queryJudgments.get(docId))
//            val query = queries.getOrElse(queryId, Seq())
//            if (doc != null && query.size > 0) {
//
//              val numQueryEntities = query.size
//
//
//              val kbLinks = FaccAnnotationsFromDocument.extractFaccAnnotations(doc).map(_.freebaseId)
//              val docEntityStats = linkSequenceToStatistics(kbLinks)
//              docAnnotationStats += docEntityStats
//
//              val linkCounts = kbLinks.groupBy(x => x).mapValues(_.length).withDefaultValue(0)
////              println("num entities: " + kbLinks.size)
////              val topLinks = linkCounts.toList.sortBy(-_._2) take 10
////              println("Top entities:\n" + topLinks.mkString("\n"))
//
//              var found = false
//              var totalEntityCount = 0
//              for (entity <- query) {
//                val entityCount = linkCounts(entity.freebaseId)
//             //   println("query: " + entity + " freq:" + entityCount)
//                if (entityCount > 0) {
//                  found = true
//                  totalEntityCount += entityCount
//                }
//
//              }
//              if (found) {
//                docsWithEntity += 1
//              }
//
//              println(s"$queryId\t$docId\t$totalEntityCount")
//
//            } else {
//            //  println("facc doc not found: " + docId)
//              numMissingDocs += 1
//              if (docId.contains("enwp")) {
//                missingWiki += 1
//              }
//            }
//            numRel += 1
//          }
//        }
//        totalMissing += numMissingDocs
//        totalDocs += numRel
//        totalWithEntity += docsWithEntity
//        totalWiki += missingWiki
//        //println(queryId + "\t" + numRel + "\t" + numMissingDocs  + "\t" + missingWiki + "\t" + docsWithEntity)
//      }
//
//      allQueryKbLinks += (queryId -> docAnnotationStats.flatten)
//    }
//  println("Total rel: " + totalDocs + " total rel wiki:" + totalWiki + " all rel missing: " + totalMissing + " total with entity:" + totalWithEntity)
//  val aggregateEntityStats = aggregateDocumentEntityStatistics(allQueryKbLinks.toSeq)
//  writeStatsFile(aggregateEntityStats, "./data/analysis/clueb-qrel-entities")
//
//  case class AnnotationStat(val entityId:String, val frequency:Int, val numDocs:Int, val prob:Double)
//
//  def linkSequenceToStatistics(annotations : Seq[String]) =  {
//
//    val size = annotations.size
//    val linkCounts = annotations.groupBy(x => x).mapValues(_.length)
//    val stats = for ((entity, count) <- linkCounts) yield {
//      AnnotationStat(entity, count, 1, (count.toDouble / size))
//    }
//    val sorted = stats.toSeq.sortBy(-_.prob)
//    sorted
//  }
//
//  def aggregateDocumentEntityStatistics(annotations : Seq[(Int,Seq[AnnotationStat])]) =  {
//
//    // num docs
//    val entityByQuery = for ( (query, entityStats) <- annotations) yield {
//
//    val numDocs = entityStats.size
//    val statsByEntity = entityStats.groupBy(_.entityId)
//    val stats = for ((entity, stats) <- statsByEntity) yield {
//      val frequency = stats.map(_.frequency).sum
//      val prob = stats.map(_.prob).sum / numDocs.toDouble
//      AnnotationStat(entity, frequency, stats.size, prob)
//    }
//    val sorted = stats.toSeq.sortBy(-_.prob)
//    query -> sorted
//    }
//    entityByQuery
//  }
//
//  /**
//   * Takes a series  of (query, doc, relevance) and writes a qrel file.
//   *
//   */
//  def writeStatsFile(entityStats: Seq[(Int, Seq[AnnotationStat])], file: String) {
//
//    val p = new PrintWriter(file, "UTF-8")
//    for ((topic, stats) <- entityStats; stat <- stats) {
//      val string = (s"$topic\t${stat.entityId}\t${Freebase2WikipediaMap.freebaseId2WikiTitleMap(stat.entityId)}\t${stat.frequency}\t${stat.numDocs}\t${stat.prob}")
//      p.println(string)
//    }
//    p.close
//  }
//
//}
