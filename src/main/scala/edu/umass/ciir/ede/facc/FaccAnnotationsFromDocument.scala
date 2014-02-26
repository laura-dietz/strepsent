package edu.umass.ciir.ede.facc

import scala.xml.XML

/**
 * Created by jdalton on 1/2/14.
 */
object FaccAnnotationsFromDocument {

  def extractKbLinks(text:String) = {

    val fixedBody = "<document>" + text + "</document>"
    val bodyXML = XML.loadString(fixedBody)
    val links = bodyXML \\ "kbLinks"
    links.map(_.text)
  }

  def extractFaccAnnotations(metadata:Map[String,String], documentname:String) = {
    val rawAnnotations = metadata.get("raw-annotations")
    rawAnnotations.map( rawAnnotations => {
    val lines = rawAnnotations.split("\\n")
    val annotations = FreebaseAnnotationReader.annotationsFromStrings(lines.iterator).get(documentname).getOrElse(Seq())
    annotations
    })
  }

  def isWikiDoc(docId:String):Boolean = {
    val components = docId.split("-")
    components(1).replace("en", "").contains("wp")
  }

  def hasFaccAnnotation(metadata:Map[String,String]):Boolean = {
    metadata.get("raw-annotations") != null
  }

}
