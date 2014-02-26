//package edu.umass.ciir.ede.elannotation
//
//import scala.collection.mutable.ListBuffer
//import org.lemurproject.galago.core.retrieval.ScoredDocument
//import java.io.{PrintWriter, File}
//import scala.Some
//import edu.umass.ciir.models.{TermEntry, LanguageModel}
//import edu.umass.ciir.util.TextNormalizer
//import scala.collection.JavaConversions._
//import edu.umass.ciir.ede.trec._
//
///**
// * Created with IntelliJ IDEA.
// * User: jdalton
// * Date: 8/27/13
// * Time: 6:01 PM
// */
//object DocJudgments2EntityQrel extends App {
//
//  val annotationDir = "/usr/aubury/scratch1/jdalton/data/robust04-nlp-annotations-factkb1"
//
//  val queryFile = "./data/queries/rob04.titles.tsv"
//
//  // if we need to limit the qrels to a set of queries
//  val entityEvalQueries =  QueryFileLoader.loadTsvQueries(queryFile)
//
//  //EntityQueryFileLoader.loadTsvFile().toSeq
//  val querySet = entityEvalQueries.map(_._1+"").toSet
//
//  val judgments = QrelLoader.fromTrec("./data/qrels/robust04.qrels")
//  val limitedJudgments = judgments filterKeys querySet
//
//
//  val nameJudgments = ListBuffer[EntityJudgment]()
//  val linkJudgments = ListBuffer[EntityJudgment]()
//
//  val entityQrelDirBase = "./data/entity-qrels"
//
//  val nilThreshold = 0.5
//  val numEntitiesRel = 10000
//
//  new File(entityQrelDirBase).mkdirs()
//
//  val docsRequiringAnnotation = ListBuffer[String]()
//
//
//
//  for (query <- entityEvalQueries) {
//      val relJudgments = limitedJudgments(query._1+"").filter(_.relevanceLevel > 0).map(j => new ScoredDocument(j.objectId, -1, j.relevanceLevel.toDouble))
//      println(query + " Rel judgments: " + relJudgments.length)
//      val judgmentAnnotations = loadAnnotations(relJudgments)
//      val trueJudgments = annotations2EntityModel(query._1+"", judgmentAnnotations)
//      nameJudgments ++= trueJudgments._1
//      linkJudgments ++= trueJudgments._2
//  }
//
//    writeQrelFile(nameJudgments, entityQrelDirBase + File.separator + "names_" + nilThreshold + "_" + numEntitiesRel + ".qrel")
//    writeQrelFile(linkJudgments, entityQrelDirBase + File.separator +  "links_" + nilThreshold + "_" + numEntitiesRel + ".qrel")
//
//
//  println("num docs requiring annotation: " + docsRequiringAnnotation.size)
//
//  def loadAnnotations(docs: Iterable[ScoredDocument]) = {
//    var numExist = 0
//    val annotatedEntities: Iterable[(ScoredDocument, Option[Seq[LinkedMention]])] = for (doc <- docs) yield {
//      val annotationFile = new File(annotationDir + File.separatorChar + doc.documentName + ".xml")
//      if (annotationFile.exists()) {
//        numExist += 1
//        try {
//          val linkedEntities = EntityAnnotationLoader.entityLinks(doc.documentName, annotationFile)
//          //println(doc + " " +linkedEntities.length)
//          doc -> Some(linkedEntities)
//          //doc -> None
//        } catch {
//          case ex => println("ERROR Loading doc: " + doc.documentName + " " + ex.getStackTraceString)
//            doc -> None
//        }
//      } else {
//        docsRequiringAnnotation += doc.documentName
//        doc -> None
//      }
//    }
//    println("num exist: " + numExist)
//    annotatedEntities
//  }
//
//  /**
//   * Takes a series  of (query, doc, relevance) and writes a qrel file.
//   *
//   * @param assessments
//   * @param file
//   */
//  def writeQrelFile(assessments: Seq[EntityJudgment], file: String) {
//    val p = new PrintWriter(file, "UTF-8")
//    val assessmentsByEntity = assessments.groupBy(x => x.topic)
//    // throws out garbage assessments.
//    for (topic <- assessmentsByEntity.keys; assessment <- assessmentsByEntity(topic)) {
//      val string = (topic + " " + assessment.confidence + " " + assessment.entityId.replace(" ", "_") + " " + assessment.relevance)
//      p.println(string)
//    }
//    p.close
//  }
//
//  def annotations2EntityModel(topic: String, docsWithEntities: Iterable[(ScoredDocument, Option[Seq[LinkedMention]])]) = {
//
//    val globalEntityNameModel = new LanguageModel(1)
//    val globalEntityLinkModel = new LanguageModel(1)
//
//    val localEntityNameModels = ListBuffer[(Double, LanguageModel)]()
//    val localEntityLinkModels = ListBuffer[(Double, LanguageModel)]()
//
//    for ((doc, linkedEntities) <- docsWithEntities) {
//      val linkedMentions = linkedEntities.getOrElse(Seq())
//      val localEntityNameModel = new LanguageModel(1)
//      val localEntityLinkModel = new LanguageModel(1)
//
//      for (mention <- linkedMentions) {
//
//        val cleanerName = mention.mention.entityName.replace("\n", " ").replace("P>", "").replace("</P", "").replace("<P", "").replace("TEXT>", "").replace("F>", "")
//        val name = TextNormalizer.normalizeText(cleanerName)
//        localEntityNameModel.addEntry(new TermEntry(name, 1, 1))
//
//        val first = mention.entityLinks.headOption
//        first match {
//          case Some(entity) => if (entity.score > nilThreshold) {
//            localEntityLinkModel.addEntry(new TermEntry(entity.wikipediaTitle, 1, 1))
//          }
//          case None => {}
//        }
//      }
//      localEntityLinkModel.calculateProbabilities()
//      localEntityNameModel.calculateProbabilities()
//
//      localEntityNameModels += ((doc.score, localEntityNameModel))
//      localEntityLinkModels += ((doc.score, localEntityLinkModel))
//    }
//
//    localEntityNameModels.map(lm => globalEntityNameModel.addDocument(lm._2))
//    localEntityLinkModels.map(lm => globalEntityLinkModel.addDocument(lm._2))
//
//    globalEntityNameModel.calculateProbabilities()
//    globalEntityLinkModel.calculateProbabilities()
//
//    println("Top entities. total: " + globalEntityNameModel.getEntries.size())
//    globalEntityNameModel.getSortedTermEntries take 0 map (t => println(t.getTerm, t.getFrequency, t.getDocumentFrequency, t.getProbability))
//
//    println("Top entity prob. ")
//    val nameProbs = probSum(globalEntityNameModel, localEntityNameModels).toSeq.sortBy(-_._2) take numEntitiesRel
//    val relNameTuples = nameProbs.map(p => EntityJudgment(topic, p._1.getTerm, 1, p._2))
//
//    //nameProbs.map(p => println(p))
//
//    println("\nTop wikipedia links total: " + globalEntityLinkModel.getEntries.size())
//    globalEntityLinkModel.getSortedTermEntries take 0 map (t => println(t.getTerm, t.getFrequency, t.getDocumentFrequency, t.getProbability))
//
//    println("Top wikipedia link prob:")
//    val linkProbs = probSum(globalEntityLinkModel, localEntityLinkModels).toSeq.sortBy(-_._2) take numEntitiesRel
//    val relLinkTuples = linkProbs.map(p => EntityJudgment(topic, p._1.getTerm, 1, p._2))
//    //linkProbs.map(p => println(p))
//    (relNameTuples, relLinkTuples)
//  }
//
//  def probSum(globalModel: LanguageModel, localModels: Seq[(Double, LanguageModel)]) = {
//
//    val result = for (gTerm <- globalModel.getEntries) yield {
//
//      var probSum = 0.0
//      for ((score, lm) <- localModels) {
//        val prob = if (lm.getTermEntry(gTerm.getTerm) != null) {
//          lm.getTermEntry(gTerm.getTerm).getProbability
//        } else {
//          0.0
//        }
//        probSum += (score * prob * (1.0 / localModels.size))
//      }
//      (gTerm, probSum)
//    }
//    result
//  }
//
//}
//
//case class EntityJudgment(topic: String, entityId: String, relevance: Int, confidence: Double)
//
