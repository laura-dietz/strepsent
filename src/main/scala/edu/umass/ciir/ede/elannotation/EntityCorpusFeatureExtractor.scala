//package edu.umass.ciir.ede.elannotation
//
//import java.io.{PrintWriter, File}
//import scala.collection.JavaConversions._
//import edu.umass.ciir.ede.features.CategoryFeatureExtractor
//import edu.umass.ciir.ede.util.TermCountsMap
//import org.lemurproject.galago.core.index.disk.DiskNameReader
//import scala.collection.mutable.ListBuffer
//
///**
// * Created by jdalton on 1/21/14.
// */
//object EntityCorpusFeatureExtractor extends App {
//
//  val (annotationCollFreq, robustEntityCounts) = TermCountsMap.loadMap(EdeConfig.robustEntityCounts)
//
//  val uniqueEntities = robustEntityCounts.keys.toSet
//
//  val p = new Parameters()
//  p.set("text", true)
//  p.set("tokenize", true)
//  p.set("metadata", true)
//
//  val corpusPath = "/home/jdalton/scratch1/full-wiki-stanf3_context_g351/corpus"
//
//  val namesPath = "/home/jdalton/scratch1/full-wiki-stanf3_context_g351/names"
//  val namesReader: DiskNameReader = new DiskNameReader(namesPath)
//  val namesItr = namesReader.getIterator
//
//  val internalIds = ListBuffer[Long]()
//  while (!namesItr.isDone) {
//    val id = namesItr.getCurrentIdentifier
//    val name = namesItr.getCurrentName
//    if (uniqueEntities.contains(name)) {
//      internalIds += id
//    }
//    namesItr.nextKey()
//  }
//  namesReader.close()
//
//  val internalIdSet = internalIds.toSet
//  println("Num ids: " + internalIdSet.size)
//
//  val categoryPw = new PrintWriter("./data/feats/robust-category-feats-all")
//  val typesPw = new PrintWriter("./data/feats/robust-type-feats-all")
//
//  val reader: DocumentReader = new CorpusReader(corpusPath)
//
//  val iterator: DocumentReader.DocumentIterator = reader.getIterator.asInstanceOf[DocumentReader.DocumentIterator]
//  val dc: Document.DocumentComponents = new Document.DocumentComponents(p)
//
//  var numDocs = 0
//  while (!iterator.isDone) {
//
//    val curDoc = iterator.getKeyString.toLong
//
//   // if (internalIdSet.contains(curDoc)) {
//
//      if (numDocs%100 == 0) {
//        println(s"$numDocs #IDENTIFIER: " + curDoc)
//      }
//      val document: Document = iterator.getDocument(dc)
//      val categories = CategoryFeatureExtractor.extractCategories(document)
//      categoryPw.println(s"${document.name}\t${categories.mkString("\t")}")
//      val types = CategoryFeatureExtractor.extractTypes(document)
//      typesPw.println(s"${document.name}\t${types.mkString("\t")}")
//      numDocs += 1
//    //}
//
//    iterator.nextKey
//  }
//  reader.close
//
//  categoryPw.close()
//  typesPw.close()
//
//
//}
