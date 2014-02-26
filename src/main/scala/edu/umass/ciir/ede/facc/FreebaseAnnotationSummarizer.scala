package edu.umass.ciir.ede.facc

import java.io.{PrintWriter, File}
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/**
 * Created with IntelliJ IDEA.
 * User: jdalton
 * Date: 8/22/13
 * Time: 2:37 PM
 * To change this template use File | Settings | File Templates.
 */
object FreebaseAnnotationSummarizer {

  val baseAnnotationPath = "./annotations/clueweb09"

  val annotationFiles = {
    val f = io.Source.fromFile("./annotations/annotationFileMap")
    val annotationFiles = f.getLines().toList
    f.close()
    annotationFiles
  }

  def docIdToAnnotationFile(clueweb09DocId: String): File = {

    val components = clueweb09DocId.split("-")

    val chunk = components(1)
    val block = components(2)
    val pattern = chunk + File.separator + block + ".anns.tsv"
    val file = annotationFiles.find(_.contains(pattern)).get

    val filePath = baseAnnotationPath + File.separator + file

    new File(filePath)

  }


  def extractAnnotationsForDocs(workingSet: Set[String], outputPath: String) {

    val pw = new PrintWriter(outputPath)
    findFiles(new File(baseAnnotationPath))
    for (f <- files) {
      println("Opening file: " + f)
      val annotations = FreebaseAnnotationReader.loadAnnotationFile(f, Some(workingSet))
      val all = annotations.values.flatten
      for (a <- all) {
        pw.println(Seq(a.documentName, a.encoding, a.entityMention, a.beginByteOffset, a.endByteOffset, a.mentionContextPosterior, a.contextOnlyPosterior, a.freebaseId).mkString("\t"))
      }
    }
    pw.close()
  }

  val files = ListBuffer[File]()

  def findFiles(path: File) {
    if (path.isDirectory) {
      path.listFiles().map(f => findFiles(f))
    } else {
      if (path.getName.endsWith(".anns.tsv")) {
        files += path
      }
    }
    //path.listFiles() ++ path.listFiles.filter(_.isDirectory).flatMap(findFiles)
  }

}

object AnnotationReaderTest extends App {


  val documentsToAnnotate = mutable.HashSet[String]()

  //  val runFiles = Array("./data/sjh-runs/clueb-sdm-o2-u2-titles.all.join", "./data/bendersky-runs/clueb.descs.lce.join")
  //  for (rf <- runFiles) {
  //    val runFile = new File(rf)
  //    val pooledDocs = RunFileLoader.readRunFileWithQuery(runFile, 1000).values.flatten.map(_.documentName)
  //    documentsToAnnotate ++= pooledDocs
  //    println(documentsToAnnotate.size)
  //  }


  //  val judgments = QrelLoader.fromTrec("./data/qrels/clue2012.qrels").values.flatten.map(_.objectId)
  //  documentsToAnnotate ++= judgments
  //  println(documentsToAnnotate.size)

  //FreebaseAnnotationSummarizer.extractAnnotationsForDocs(documentsToAnnotate.toSet, FreebaseAnnotationSummarizer.baseAnnotationPath + File.separator + "qrels")
  val file = FreebaseAnnotationSummarizer.docIdToAnnotationFile("clueweb09-enwp01-75-20596")
  //  val allAnnotations = FreebaseAnnotationReader.loadAnnotationFile(file, None)

  //  println(allAnnotations.size)

  val annotations = FreebaseAnnotationReader.loadAnnotationFile(file, Some(Set("clueweb09-enwp01-75-20596")))
  annotations.map(a => println(a._1 + " " + a._2.map(link => (link.freebaseId, link.entityMention, link.wikipediaTitle))))

}
