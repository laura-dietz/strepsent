package edu.umass.ciir.ede.facc

import scala.collection.mutable.ListBuffer
import edu.umass.ciir.ede.elannotation._
import edu.umass.ciir.ede._
import edu.umass.ciir.kbbridge.data.EntityRepr
import edu.umass.ciir.strepsi.{SeqTools, ScoredDocument}

/**
 * User: dietz
 * Date: 8/23/13
 * Time: 2:45 PM
 */
class FaccEntitLinkFinder  {
  var numMissing = 0
  var numMissingFromEmptyAnnotations = 0
  val missingList = new ListBuffer[String]()

  def findEntitiesInDoc(scoredDoc: ScoredDocument, getReprs: () => Map[WikiTitle, EntityRepr],
                        getTitleNormalizationMap: () => Map[WikiTitle, WikiTitle]):Map[WikiTitle,Int] = {
    val documentName = scoredDoc.documentName

    val annotationFile = FreebaseAnnotationSummarizer.docIdToAnnotationFile(documentName)
    println("loading FACC annotations from annotationFile = "+annotationFile)
    val docName2Ann = FreebaseAnnotationReader.loadAnnotationFile(annotationFile, Some(Set(documentName)))
    docName2Ann.map(a => {
      println(a._1 + " " + a._2.map(link => (link.freebaseId, link.entityMention, link.wikipediaTitle)))
    })

    val wikititleCount:Map[WikiTitle,Int] =
      docName2Ann.headOption match {
        case Some(Pair(_, annotations)) => {
          println("annotations for doc: " + scoredDoc.documentName)
          annotations.map(a => println(a + " wiki:" + a.wikipediaTitle))
          val wikiTitleSeq = annotations.map(_.wikipediaTitle).filter(_.trim().length>2)
          if (wikiTitleSeq.length>0)
            SeqTools.countMap[WikiTitle](wikiTitleSeq)
          else  {
            numMissing += 1
            numMissingFromEmptyAnnotations += 1
            missingList += documentName
            Map.empty
          }
        }
        case None => {
          numMissing += 1
          missingList += documentName

          Map.empty
        }
      }
    wikititleCount
  }

  def outputMissingAnnotations() {
    System.err.println("missing annotations "+numMissing+" missing from emptyAnnotations "+numMissingFromEmptyAnnotations)

    System.err.println("documents with missing annotations: "+missingList.mkString("\n"))
    numMissing = 0
  }

}
