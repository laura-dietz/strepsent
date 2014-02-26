//package edu.umass.ciir.ede.elannotation
//
//import java.io.File
//import collection.mutable
//import java.io.PrintWriter
//import edu.umass.ciir.ede.trec.{QrelLoader, RunFileLoader}
//
//
///**
// * User: jdalton
// * Date: 6/6/13
// */
//object AnnotationScriptGenerator extends App {
//
//
//  val documentsToAnnotate = mutable.HashSet[String]()
//
//  val runFiles = Array("./data/bendersky-runs/rob04.titles.mse.10.run","./data/sjh-runs/rob04-sdm-o2-u2-titles.all.join", "./data/simplesketch/robust-robust-title-naive-document-sdm-1000.run", "./data/simplesketch/robust-robust-title-naive-maxpsg-sdm-1000.run")
//  for (rf <- runFiles) {
//    val runFile = new File(rf)
//    val pooledDocs = RunFileLoader.readRunFileWithQuery(runFile, 100).values.flatten.map(_.documentName)
//    documentsToAnnotate ++= pooledDocs
//    println(documentsToAnnotate.size)
//  }
//
//
//  val judgments = QrelLoader.fromTrec("./data/qrels/robust04.qrels").values.flatten.filter(j => j.relevanceLevel > 0).map(_.objectId)
//  documentsToAnnotate ++= judgments
//  println(documentsToAnnotate.size)
//
//  writeAnnotationScript(documentsToAnnotate)
//
//  def writeAnnotationScript(docsRequiringAnnotation: Iterable[String]) = {
//    val outputFile = new File("./scripts/annotate-rob04-swarm-factkb1-all")
//    val n = 100000
//    var curBatch = 0
//    var p = new PrintWriter(outputFile.getAbsolutePath() + curBatch.toString + ".sh", "UTF-8")
//    for ((docSet, idx) <- (docsRequiringAnnotation grouped 5).zipWithIndex) {
//      val sb = new StringBuilder
//      if (idx % n == 0 && idx > 0) {
//        p.close
//        curBatch += 1
//        p = new PrintWriter(outputFile.getAbsolutePath() + curBatch.toString + ".sh", "UTF-8")
//      }
//
//      sb append "qsub -b y " + "-l mem_free=4G -l mem_token=4G" + " -cwd -o ./out/"
//      sb append docSet.head
//      sb append " -e ./err/"
//      sb append docSet.head
//
//      sb append " ./scripts/runAnnotation.sh "
//      //  sb append " /work1/allan/jdalton/tacco/scripts/runEntityLinker.sh "
//
//      // input query
//      sb append docSet.mkString(",")
//      sb append " ./data/indices/robust04-g34"
//      sb append " ./robust04-nlp-annotations-factkb1"
//
//      // println(sb.toString)
//      p.println(sb.toString)
//    }
//    p.close()
//  }
//}
