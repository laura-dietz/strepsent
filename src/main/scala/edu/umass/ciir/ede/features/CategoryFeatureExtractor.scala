package edu.umass.ciir.ede.features

import edu.umass.ciir.ede.elannotation.AnnotatedDocument
import edu.umass.ciir.strepsent._
import edu.umass.ciir.strepsi.galagocompat.GalagoTag

import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer

/**
* Created by jdalton on 1/23/14.
*/
class CategoryFeatureExtractor(val freebaseId2WikiTitleMap:Map[String, String]) {

  val stopTypes:Set[FreeBaseType] = Set("/business/employer",
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
                            nilThreshold: Double = 0.5)
  : Map[EntityId, (Seq[Category], Seq[FreeBaseType])]= {
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


  /**
   * Returns the union of categories and types across all entity links in the annotated document
   * @param annotations
   * @param nilthreshold
   * @param typesAndCategories
   * @return
   */
  def extractCategoriesAndTypes(annotations: AnnotatedDocument, nilthreshold: Double = 0.5, typesAndCategories: Map[EntityId, (Seq[Category], Seq[FreeBaseType])]): (Seq[Category], Seq[FreeBaseType]) = {
    // todo why return a set and not a distribution?
    val nonNilEntities = annotations.kbLinks.map(_.entityLinks take 1).flatten.filter(_.score > nilthreshold)
    val entityIds = nonNilEntities.map(_.wikipediaTitle)

    val entitiesWithTypes = for (entity <- entityIds) yield {
      typesAndCategories(entity)
    }

    val categories = entitiesWithTypes.map(_._1).flatten.distinct
    val typeTerms = entitiesWithTypes.map(_._2).flatten.distinct
    (categories, typeTerms)
  }

  def pullCategoriesForEntitiesFromDisk(entitySet: Set[EntityId]): Map[EntityId, (Seq[Category], Seq[FreeBaseType])] = {

    println("Extracting categories for entities: " + entitySet.size)

    val wikipediaTitleSet = entitySet.map(id => {
      if (id.startsWith("/m")) {
        // we have a facc annotation entity; map this to a wikipedia entity.
        freebaseId2WikiTitleMap.get(id)
      } else {
        Some(id)
      }
    }).flatten

    val titleSet = wikipediaTitleSet.toSet

    println("Reading category map. For title set size: " + titleSet.size)
    val categoryFile = "./data/stats/wikipedia-title2categories"
    val source = io.Source.fromFile(categoryFile)
    val lines = source.getLines()
    val categories = for (l <- lines if (titleSet.contains(l.split("\t")(0)))) yield {
      val fields = l.split("\t")
      val title:EntityId = fields(0)

      val categories = fields.drop(1).distinct.toSeq
      title -> categories
    }
    val categoryMap = categories.toMap.withDefaultValue(Seq[Category]())

    println("Reading type map.")
    val fbTypeFile = "./data/stats/wikipedia-title2fbtypes"
    val typeSource = io.Source.fromFile(fbTypeFile)
    val typeLines = typeSource.getLines()
    val fbTypes = for (t <- typeLines if (titleSet.contains(t.split("\t")(0)))) yield {
      val fields = t.split("\t")
      val title:EntityId = fields(0)
      val t1 = fields.drop(1).distinct.toSeq
      title -> t1
    }
    val fbTypeMap = fbTypes.toMap.withDefaultValue(Seq[FreeBaseType]())

    val types = for ((entityId, idx) <- entitySet.toSeq.zipWithIndex) yield {
      val wikipediaEntity = if (entityId.startsWith("/m")) {
        // we have a facc annotation entity; map this to a wikipedia entity.
        freebaseId2WikiTitleMap.get(entityId) // todo use disk-back-map instead to save RAM
      } else {
        Some(entityId)
      }
      wikipediaEntity match {
        case Some(wikipediaEntity) => {
          val categories = categoryMap(wikipediaEntity)
          val types = fbTypeMap(wikipediaEntity)
          entityId ->(categories, types)
        }
        case None => entityId ->(Seq.empty, Seq.empty)
      }
    }

    val entityToTypeMap = types.toMap[EntityId, (Seq[Category],Seq[FreeBaseType])]
    entityToTypeMap
  }

  def pullCategoriesForEntities(entityDocumentPuller:(DocumentName) => (Seq[String], Seq[GalagoTag]),
                                entitySet: Set[EntityId]): Map[EntityId, (Seq[Category], Seq[FreeBaseType])] = {

    println("Extracting categories for entities: " + entitySet.size)

    val types = for ((id, idx) <- entitySet.toSeq.zipWithIndex) yield {
      if (idx % 20 == 0) {
        println(s"reading cats for entity: $idx\t$id")
      }

      val wikipediaEntity = if (id.startsWith("/m")) {
        // we have a facc annotation entity; map this to a wikipedia entity.
        println("getWikititle from titlemap "+id+" -> "+freebaseId2WikiTitleMap.get(id))
        freebaseId2WikiTitleMap.get(id)
      } else {
        println("was already wiki title")
        Some(id)
      }

      if (wikipediaEntity.isDefined) {
        try{
          val entityDoc = entityDocumentPuller(wikipediaEntity.get.trim.asInstanceOf[DocumentName])
            val categories = extractCategories(entityDoc._1, entityDoc._2)
            val types = extractTypes(entityDoc._1, entityDoc._2)
            id ->(categories, types)
        } catch {
          case ex : RuntimeException => {
            println("unable to get categories for entity: " + wikipediaEntity.get
            + " because " + ex.getMessage)
            id ->(Seq[Category](), Seq[FreeBaseType]())
          }
        }
      } else {
        id ->(Seq[Category](), Seq[FreeBaseType]())
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

  def extractCategories(terms:Seq[Category], fields:Seq[GalagoTag]): Seq[Category] = {

    val fieldsToCount = Set("category")

    val tokenMap = scala.collection.mutable.HashMap[String, ListBuffer[Category]]()
    for (field <- fields) {
      if (fieldsToCount contains field.name) {
        tokenMap.getOrElseUpdate(field.name, ListBuffer[Category]()) ++= terms.subList(field.begin, field.end)

      }
    }
    val categories = tokenMap.getOrElse("category", ListBuffer[Category]())
    categories.distinct
  }

  def extractTypes(terms:Seq[FreeBaseType], fields:Seq[GalagoTag]): Seq[FreeBaseType] = {

    val fieldsToCount = Set("fbtype")

    val tokenMap = scala.collection.mutable.HashMap[String, ListBuffer[FreeBaseType]]()
    for (field <- fields) {
      if (fieldsToCount contains field.name) {
        tokenMap.getOrElseUpdate(field.name, ListBuffer[FreeBaseType]()) ++= terms.subList(field.begin, field.end)

      }
    }
    val types = tokenMap.getOrElse("fbtype", ListBuffer[FreeBaseType]())
    types.distinct
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
