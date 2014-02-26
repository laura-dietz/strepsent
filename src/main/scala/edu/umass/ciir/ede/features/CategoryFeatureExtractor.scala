package edu.umass.ciir.ede.features

import edu.umass.ciir.ede.elannotation.{EntityAnnotationLoader, AnnotatedDocument}
import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer
import java.io.File
import edu.umass.ciir.ede.facc.Freebase2WikipediaMap
import edu.umass.ciir.strepsi.{LogTools, ScoredDocument}
import edu.umass.ciir.strepsi.galagocompat.GalagoTag

/**
* Created by jdalton on 1/23/14.
*/
object CategoryFeatureExtractor {

  val stopTypes = Set("/business/employer",
    "/organization/organization",
    "/location/location",
    "/base/ontologies/ontology_instance",
    "/base/argumentmaps/topic",
    "/base/argumentmaps/thing_of_disputed_value",
    "/location/dated_location",
    "/location/statistical_region",
    "/base/biblioness/bibs_location",
    "/book/book_subject",
    "/guid/9202a8c04000641f8000000009208ca4",
    "/location/hud_foreclosure_area",
    "/base/tagit/concept",
    "/organization/organization_member",
    "/business/business_operation",
    "/user/tsegaran/random/taxonomy_subject",
    "/people/deceased_person",
    "/user/maxim75/default_domain/dbpedia_import",
    "/user/narphorium/people/topic",
    "/user/narphorium/people/nndb_person",
    "/people/person",
    "/periodicals/newspaper_circulation_area",
    "/user/tsegaran/random/taxonomy_subject",
    "/location/administrative_division",
    "/base/aareas/schema/administrative_area",
    "/base/biblioness/bibs_topic",
    "/base/masterthesis/topic",
    "/freebase/equivalent_topic",
    "/base/languagesfordomainnames/topic",
    "/base/popstra/sww_base",
    "/base/popstra/topic",
    "/user/skud/names/namesake",
    "/base/todolists/topic",
    "/organization/organization_scope",
    "/base/skosbase/topic",
    "/base/skosbase/vocabulary_equivalent_topic",
    "/user/skud/flags/flag_having_thing",
    "/base/tagit/place",
    "/base/popstra/location",
    "/time/day_of_year",
    "/time/month",
    "/base/localfood/seasonal_month",
    "/base/popstra/sww_base",
    "/base/popstra/topic",
    "/user/pak21/splitter/split_completed",
    "/government/governmental_jurisdiction",
    "/user/skud/names/topic",
    "/base/tagit/place",
    "/user/narphorium/default_domain/base_equivalent_location",
    "/user/skud/names/namesake",
    "/base/popstra/location",
    "/user/skud/flags/flag_having_thing",
    "/user/skud/flags/topic",
    "/user/skud/names/topic",
    "/user/skud/names/name_source",
    "/user/hedgehog/default_domain/verifiable_statement_subject",
    "/organization/organization_scope",
    "/sports/sports_team_location",
    "/base/plopquiz/topic",
    "/film/film_location",
    "/base/localfood/topic",
    "/user/robert/military/topic",
    "/user/robert/military/military_power",
    "/time/day_of_week",
    "/food/beer_country_region",
    "/base/petbreeds/topic",
    "/fictional_universe/fictional_setting",
    "/user/robert/military/topic",
    "/user/robert/military/military_power",
    "/olympics/olympic_participating_country",
    "/base/datedlocationtest/topic",
    "/base/datedlocationtest/dated_location_test",
    "/user/gmackenz/public_gmackenz_types/monthly_calendar_event_early_mid_late",
    "/fictional_universe/fictional_setting"
  )

  def createCategoryTypeMap(documents: Seq[String], annotations: Map[String, AnnotatedDocument],
//                            searcher: GalagoSearcher,
                            nilThreshold: Double = 0.5): Map[String, (Set[String], Set[String])] = {
    val entities = for ((doc, idx) <- documents.zipWithIndex) yield {
      val docAnnotationOption = annotations.get(doc)

      val entities = if (docAnnotationOption == None) {
        Seq[String]()
      } else {
        val nonNilEntities = docAnnotationOption.get.kbLinks.map(_.entityLinks take 1).flatten.filter(_.score > nilThreshold)
        val entityIds = nonNilEntities.map(_.wikipediaTitle).toSeq
        entityIds
      }
      entities
    }
    val entitySet = entities.flatten.toSet

    val categoriesAndTypes = pullCategoriesForEntitiesFromDisk(entitySet)
    categoriesAndTypes
  }

//  def extractEntityAndTextFeatures(rawTextQuery: String, annotations: Map[String, AnnotatedDocument],
//                                   documents: Seq[ScoredDocument], nilThreshold: Double = 0.5,
//                                   categoryAndTypeCounts: Map[String, (Set[String], Set[String])],
//                                   numFeedbackTerms: Int = 25, fb: FeedbackParams) = {
//
//    val filteredSd = documents.filter(sd => annotations.contains(sd.documentName)) take fb.numFeedbackDocs
//
//    if(filteredSd.isEmpty){
//      Map("categories" -> Seq.empty, "types" -> Seq.empty)
//    } else {
//      val docFeatureMap = extractCategories(annotations, filteredSd, categoryAndTypeCounts, fb)
//
//      val docProbs = LogTools.logExpSumNormalize[ScoredDocument](filteredSd, _.score).map(e => e._1.documentName -> e._2).toMap
//
//      val categoryDocMap = docFeatureMap.map(r => r._1 -> r._2._1).toMap
//      val categoryDistribution = ExpansionModels.relevanceModel(filteredSd.map(_.documentName), docProbs, categoryDocMap)
//
//      val typeDocMap = docFeatureMap.map(r => r._1 -> r._2._2).toMap
//      val typeDistribution = ExpansionModels.relevanceModel(filteredSd.map(_.documentName), docProbs, typeDocMap)
//      val filteredTypes = typeDistribution.filterNot(t => stopTypes.contains(t._1))
//
//      val featureMap = Map("categories" -> categoryDistribution, "types" -> filteredTypes)
//      featureMap
//    }
//  }
//
//  def extractCategories(annotations: Map[String, AnnotatedDocument], documents: Seq[ScoredDocument],
//                        categoryAndTypeCounts: Map[String, (Set[String], Set[String])],
//                        fb: FeedbackParams): Seq[(String, (Seq[String], Seq[String]))] = {
//
//    val docsToTypes = new ListBuffer[(String, (Seq[String], Seq[String]))]()
//    for (doc <- documents) yield {
//      val docAnnotationOption = annotations.get(doc.documentName)
//      val types = if (docAnnotationOption == None) {
//        (Seq[String](), Seq[String]())
//      } else {
//        val typesAndCats = extractCategoriesAndTypes(docAnnotationOption.get, 0.5, categoryAndTypeCounts)
//        typesAndCats
//      }
//      docsToTypes += doc.documentName -> types
//    }
//    return docsToTypes
//  }


  def extractCategoriesAndTypes(annotations: AnnotatedDocument, nilthreshold: Double = 0.5, typesAndCategories: Map[String, (Set[String], Set[String])]): (Seq[String], Seq[String]) = {
    val nonNilEntities = annotations.kbLinks.map(_.entityLinks take 1).flatten.filter(_.score > nilthreshold)
    val entityIds = nonNilEntities.map(_.wikipediaTitle)

    val entitiesWithTypes = for (entity <- entityIds) yield {
      typesAndCategories(entity)
    }

    val categories = entitiesWithTypes.map(_._1).flatten
    val typeTerms = entitiesWithTypes.map(_._2).flatten
    (categories, typeTerms)
  }

  def pullCategoriesForEntitiesFromDisk(entitySet: Set[String]): Map[String, (Set[String], Set[String])] = {

    println("Extracting categories for entities: " + entitySet.size)

    val wikipediaTitleSet = entitySet.map(id => {
      if (id.startsWith("/m")) {
        // we have a facc annotation entity; map this to a wikipedia entity.
        Freebase2WikipediaMap.freebaseId2WikiTitleMap(id)
      } else {
        id
      }
    })
    val titleSet = wikipediaTitleSet.toSet

    println("Reading category map. For title set size: " + titleSet.size)
    val categoryFile = "./data/stats/wikipedia-title2categories"
    val source = io.Source.fromFile(categoryFile)
    val lines = source.getLines()
    val categories = for (l <- lines if (titleSet.contains(l.split("\t")(0)))) yield {
      val fields = l.split("\t")
      val title = fields(0)
      val categories = fields.drop(1).toSet
      title -> categories
    }
    val categoryMap = categories.toMap.withDefaultValue(Set[String]())

    println("Reading type map.")
    val fbTypeFile = "./data/stats/wikipedia-title2fbtypes"
    val typeSource = io.Source.fromFile(fbTypeFile)
    val typeLines = typeSource.getLines()
    val fbTypes = for (t <- typeLines if (titleSet.contains(t.split("\t")(0)))) yield {
      val fields = t.split("\t")
      val title = fields(0)
      val t1 = fields.drop(1).toSet
      title -> t1
    }

    val fbTypeMap = fbTypes.toMap.withDefaultValue(Set[String]())

    val types = for ((id, idx) <- entitySet.toSeq.zipWithIndex) yield {
      val wikipediaEntity = if (id.startsWith("/m")) {
        // we have a facc annotation entity; map this to a wikipedia entity.
        Freebase2WikipediaMap.freebaseId2WikiTitleMap(id)
      } else {
        id
      }
      val categories = categoryMap(wikipediaEntity)
      val types = fbTypeMap(wikipediaEntity)
      id ->(categories, types)
    }

    val entityToTypeMap = types.toMap
    entityToTypeMap
  }

  def pullCategoriesForEntities(entityDocumentPuller:(String) => (Seq[String], Seq[GalagoTag]),
                                entitySet: Set[String]): Map[String, (Set[String], Set[String])] = {

    println("Extracting categories for entities: " + entitySet.size)

    val types = for ((id, idx) <- entitySet.toSeq.zipWithIndex) yield {
      if (idx % 20 == 0) {
        println(s"reading cats for entity: $idx\t$id")
      }

      val wikipediaEntity = if (id.startsWith("/m")) {
        // we have a facc annotation entity; map this to a wikipedia entity.
        Freebase2WikipediaMap.freebaseId2WikiTitleMap(id)
      } else {
        id
      }

      if (wikipediaEntity.size > 0) {
        val entityDoc = entityDocumentPuller(wikipediaEntity)
        if (entityDoc != null) {
          val categories = extractCategories(entityDoc._1, entityDoc._2)
          val types = extractTypes(entityDoc._1, entityDoc._2)
          id ->(categories, types)
        } else {
          println("unable to get categories for entity: " + wikipediaEntity)
          id ->(Set[String](), Set[String]())
        }


      } else {
        id ->(Set[String](), Set[String]())
      }
    }

    val entityToTypeMap = types.toMap
    entityToTypeMap
  }


  //  val entitiesWithTypes = for (entity <- entitySet) yield {
  //    entityToTypeMap(entity)
  //  }
  //
  //  val categories = entitiesWithTypes.map(_._1).flatten
  //  val typeTerms = entitiesWithTypes.map(_._2).flatten
  //  (categories, typeTerms)
  //}

  def extractCategories(terms:Seq[String], fields:Seq[GalagoTag]): Set[String] = {

    val fieldsToCount = Set("category")

    val tokenMap = scala.collection.mutable.HashMap[String, ListBuffer[String]]()
    for (field <- fields) {
      if (fieldsToCount contains field.name) {
        tokenMap.getOrElseUpdate(field.name, ListBuffer[String]()) ++= terms.subList(field.begin, field.end)

      }
    }
    val categories = tokenMap.getOrElse("category", ListBuffer[String]())
    categories.toSet
  }

  def extractTypes(terms:Seq[String], fields:Seq[GalagoTag]): Set[String] = {

    val fieldsToCount = Set("fbtype")

    val tokenMap = scala.collection.mutable.HashMap[String, ListBuffer[String]]()
    for (field <- fields) {
      if (fieldsToCount contains field.name) {
        tokenMap.getOrElseUpdate(field.name, ListBuffer[String]()) ++= terms.subList(field.begin, field.end)

      }
    }
    val types = tokenMap.getOrElse("fbtype", ListBuffer[String]())
    types.toSet
  }

//  def main(args: Array[String]) {
//    val sd1 = new ScoredDocument("FBIS4-25684", 1, -5.86788031)
//    val sd2 = new ScoredDocument("FT934-6431", 2, -6.19152746)
//    val doc1 = EntityAnnotationLoader.documentModel("FBIS4-25684", new File("/usr/aubury/scratch1/jdalton/data/robust04-nlp-annotations-factkb1/FBIS4-25684.xml"))
//    val doc2 = EntityAnnotationLoader.documentModel("FT934-6431", new File("/usr/aubury/scratch1/jdalton/data/robust04-nlp-annotations-factkb1/FT934-6431.xml"))
//
//    val wikiSearcher = GalagoSearcher(wikipediaCollectionPath)
//
//    val categoryCountMap = createCategoryTypeMap(Seq(sd1, sd2).map(_.documentName), Map("FBIS4-25684" -> doc1, "FT934-6431" -> doc2), wikiSearcher, 0.5)
//    val entityFeatures = extractEntityAndTextFeatures("blah", Map("FBIS4-25684" -> doc1, "FT934-6431" -> doc2), Seq(sd1, sd2), 0.5, categoryCountMap, 25, FeedbackParams(1.0, 0, 0, 0, 25, 20, 1))
//    entityFeatures.map(f => println(s"${f._1}\t${f._2.mkString(" ")}"))
//
//  }

}
