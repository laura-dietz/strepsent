package edu.umass.ciir.ede.facc

import java.io.File
import collection.mutable.ListBuffer

/**
 * Created with IntelliJ IDEA.
 * User: jdalton
 * Date: 8/22/13
 * Time: 3:05 PM
 * To change this template use File | Settings | File Templates.
 */
object FreebaseAnnotationReader {
  def loadAnnotationFile(file: File, docIdFilter: Option[Set[String]] = None): Map[String, Seq[FreebaseEntityAnnotation]] = {
    println("FreebaseAnnotationReader \"" + docIdFilter + "\"")
    val source = io.Source.fromFile(file)
    val lines = source.getLines()
    val annotations = annotationsFromStrings(lines)
    source.close()
    annotations
  }

  def annotationsFromStrings(lines: Iterator[String], docIdFilter: Option[Set[String]] = None) = {

    val annotations = new ListBuffer[FreebaseEntityAnnotation]
    for (l <- lines if (docIdFilter == None || docIdFilter.get.contains(l.split("\\s+")(0)))) {
      val fields = l.split("\t")
      val name = fields(0)
      val encoding = fields(1)
      val mentionString = fields(2)
      val beginByteOffset = fields(3).toInt
      val endByteOffset = fields(4).toInt
      val mentionConfidence = fields(5).toDouble
      val contextConfidence = fields(6).toDouble
      val freebaseId = fields(7)
      val annotation = FreebaseEntityAnnotation(name, encoding, mentionString, beginByteOffset, endByteOffset, mentionConfidence, contextConfidence, freebaseId)
      annotations += annotation
    }
    val map = annotations.toList.groupBy(_.documentName)
    map
  }


}


