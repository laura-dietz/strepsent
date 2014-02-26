package edu.umass.ciir.ede.facc

import java.io.File
import scala.collection.mutable.ListBuffer

/**
 * Created by jdalton on 12/19/13.
 */
object FreeebaseQueryAnnotationReader {

  def loadAnnotationFile(file: File): Map[Int, Seq[FreebaseEntityQueryAnnotation]] = {
    println("FreebaseAnnotationQueryReader")
    val source = io.Source.fromFile(file)
    val lines = source.getLines()
    val annotations = annotationsFromStrings(lines)
    source.close()
    annotations
  }


  def loadRevisedAnnotations(file: File): Map[Int, Seq[FreebaseEntityQueryAnnotation]] = {
    println("FreebaseAnnotationQueryReader")
    val source = io.Source.fromFile(file)
    val lines = source.getLines()
    val annotations = new ListBuffer[FreebaseEntityQueryAnnotation]
    for (l <- lines if (!l.startsWith("#")))  {
      val fields = l.split("\t")
      val queryId = fields(0).toInt
     // val title = fields(1)
      val revisedKbEntities = fields(4).split("\\s+")
      for (entity <- revisedKbEntities) {
        if (entity.trim.length > 0) {
          val annotation = FreebaseEntityQueryAnnotation(queryId, "description", "", -1, -1,entity, -1)
          annotations += annotation
        }

      }

    }
    val map = annotations.toList.groupBy(_.topicId) //filter(_.topicDescription.startsWith("description"))

    val t = scala.collection.immutable.TreeMap(map.toArray:_*)
    source.close()
    t
  }

  def annotationsFromStrings(lines : Iterator[String]) = {

    val annotations = new ListBuffer[FreebaseEntityQueryAnnotation]

    var topicId : Int = -1
    var topicDescription = ""

    for (l <- lines if (!l.startsWith("#") && l.length() > 1)) {

      if (l.startsWith("topic-")) {
        val fields = l.split("-")
        topicId = fields(1).toInt
        if (fields.length == 3) {
          topicDescription = fields(2)
        } else {
          topicDescription = fields(2) +"-" + fields(3)
        }

      } else {
        val fields = l.split("\t")
        if (fields.length != 5) {
          println("Invalid line: " + l + " num fields: " + fields.length)
        }
        try {
        val name = fields(0)
        val beginByteOffset = fields(1).toInt
        val endByteOffset = fields(2).toInt
        val freebaseId = fields(3)
        val posterior = fields(4).toDouble

        val annotation = FreebaseEntityQueryAnnotation(topicId, topicDescription, name, beginByteOffset, endByteOffset,
          freebaseId, posterior)
        annotations += annotation
        } catch {
          case e:Exception => println("Error parsing line: " + l + " error: " + e.getMessage)
        }
      }
    }

    val map = annotations.toList.groupBy(_.topicId) //filter(_.topicDescription.startsWith("description"))

    val t = scala.collection.immutable.TreeMap(map.toArray:_*)
    t
  }

}
