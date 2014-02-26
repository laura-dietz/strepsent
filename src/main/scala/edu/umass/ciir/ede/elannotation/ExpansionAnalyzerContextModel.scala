//package edu.umass.ciir.ede.elannotation
//
//import edu.umass.ciir.ede.trec.{QueryFileLoader, RunFileLoader}
//import java.io.File
//import edu.umass.ciir.ede.EdeConfig
//import scala.collection.mutable.{ListBuffer, HashMap}
//import edu.umass.ciir.galago.GalagoSearcher
//import org.lemurproject.galago.tupleflow.Parameters
//import scala.collection.JavaConversions._
//import edu.umass.ciir.kbbridge.util.SeqTools
//import edu.umass.ciir.ede.features.DocumentScoringUtilities
//
//
///**
// * Created by jdalton on 1/11/14.
// */
//object ExpansionAnalyzerContextModel extends App {
//
//  val rf = "./data/runs/robust-robust-title-naive-document-sdm-1000.run"
//  val runFile = new File(rf)
//  val run = RunFileLoader.readRunFileWithQuery(runFile, 100)
//
//  val queryFile = EdeConfig.robustTitlesQueryFile
//  val queries = QueryFileLoader.loadTsvQueries(queryFile)
//
//  val annotationDir = "/home/jdalton/scratch1/data/robust04-nlp-annotations-factkb1"
//
//  val p = new Parameters()
//  p.set("text", true)
//  p.set("tokenize", true)
//  p.set("metadata", true)
//
//  val searcher = GalagoSearcher(EdeConfig.wikipediaCollectionPath, p)
//
//  for ((queryId, docs) <- run) {
//
//    val query = queries(queryId.toInt)
//    println("Starting query: " + queryId + " " + query)
//
//    var numMissing = 0
//    for (doc <- docs) {
//      var docScore = 0.0d
//
//      val annotationFile = new File(annotationDir + File.separatorChar + doc.documentName + ".xml")
//      if (annotationFile.exists()) {
//        val linkedEntities = EntityAnnotationLoader.entityLinks(doc.documentName, annotationFile)
//
//        val nonNilEntities = linkedEntities.map(_.entityLinks take 1).flatten.filter(_.score > 0.5).map(_.wikipediaTitle)
//        val countMap = SeqTools.countMap(nonNilEntities)
//        val uniqTitles = nonNilEntities.toSet
//        for (title <- uniqTitles) {
//          val entityDoc = searcher.getDocument(title)
//          if (entityDoc != null) {
//
//           // val entityProb = ExpansionTextAnalyzer.extractFeatures(query, Map("text" -> entityDoc.terms))
//
//            val basicIntersection = DocumentScoringUtilities.wordIntersection(query, entityDoc.terms.mkString(" "))
//
//            val intersection = basicIntersection._1
//            val prob = basicIntersection._2
//
////            val fieldTokens = ExpansionTextAnalyzer.fieldTokenMap(entityDoc)
////            val fieldProbs = ExpansionTextAnalyzer.fieldMatchFeatures(query, fieldTokens)
////
////            val maxProb = fieldProbs.map(_._2._1).max
////            val exactMatches = ExpansionTextAnalyzer.exactFieldMatchMap(query, entityDoc)
////
////            val exactMatchMax = exactMatches.map(_._2).max
//            val entityWeight = countMap(title) / nonNilEntities.size.toDouble
//          //  val score = entityWeight * entityProb("text")
//         //   docScore += score
//        //    println(s"${doc.documentName}\t$title\t$intersection\t$prob\t${entityProb("text")}\t$entityWeight\t$score")
//          }
//        }
//
//      } else {
//        numMissing += 1
//      }
//      println(s"entscore\t${doc.documentName}\t$docScore")
//    }
//  }
//
//
//
//
//}
