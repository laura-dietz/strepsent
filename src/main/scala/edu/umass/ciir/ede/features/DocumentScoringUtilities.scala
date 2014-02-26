package edu.umass.ciir.ede.features

import scala.collection.mutable.{HashMap, ListBuffer}
import scala.collection.JavaConversions._
import edu.umass.ciir.ede.{ExtentArray}
import edu.umass.ciir.strepsi.ciirshared.LanguageModel
import edu.umass.ciir.strepsi.{SetMeasures, TextNormalizer, StopWordList}
import scala.collection.mutable
import edu.umass.ciir.strepsi.termcounts.TermCollectionCountsMap
import TermCollectionCountsMap.TermCollectionCounts
import edu.umass.ciir.strepsi.galagocompat.GalagoTag

/**
 * Created by jdalton on 1/11/14.
 */
object DocumentScoringUtilities {

  def wordIntersection(query: String, text: String) = {
    val normalizedMentionName = TextNormalizer.normalizeText(query)
    val normalizedEntityName = TextNormalizer.normalizeText(text)
    // word similarity features
    val mentionWords = normalizedMentionName.split("\\s+")
    val entityWords = normalizedEntityName.split("\\s+")

    val mentionSet = mentionWords.toSet
    val wordSetSimilarity = SetMeasures(mentionSet, entityWords.toSet)
    (wordSetSimilarity.intersect, wordSetSimilarity.intersect / mentionSet.size.toDouble)
  }

  def fieldTokenMap(terms:Seq[String], tags:Seq[GalagoTag]) = {
    val fieldsToCount = Set("anchor", "title", "redirect", "fbname", "text")
    val tokenMap = scala.collection.mutable.HashMap[String, ListBuffer[String]]()

    for (reqField <- fieldsToCount) {
      tokenMap.put(reqField, ListBuffer[String]())
    }

    for (f <- tags) {
      if (fieldsToCount contains f.name) {
        val fieldTokens = terms.subList(f.begin, f.end)

        val curTokenList = tokenMap.getOrElseUpdate(f.name, ListBuffer[String]()) ++= fieldTokens
      }
    }
 //   tokenMap.put("all", new ListBuffer()++=terms)
    tokenMap.toMap
  }

  def fieldTokenMapExact(terms:Seq[String], tags:Seq[GalagoTag]) = {
    val fieldsToCount = Set("anchor", "title", "redirect", "fbname")
    val tokenMap = scala.collection.mutable.HashMap[String, ListBuffer[String]]()

    for (reqField <- fieldsToCount) {
      tokenMap.put(reqField, ListBuffer[String]())
    }

    for (f <- tags) {
      if (fieldsToCount contains f.name) {
        val fieldTokens = terms.subList(f.begin, f.end).mkString(" ")

        val curTokenList = tokenMap.getOrElseUpdate(f.name, ListBuffer[String]()) += fieldTokens
      }
    }
    //   tokenMap.put("all", new ListBuffer()++=terms)
    tokenMap.toMap
  }


  def extractFeaturesUnsmoothed(query: String, tokenMap: Map[String, ListBuffer[String]]): HashMap[String,
    Double] = {
    val queryTokens = TextNormalizer.normalizeFilterStop(query,StopWordList.isStopWord)
    val fieldProbMap = mutable.HashMap[String, Double]()

    for (field <- tokenMap.keySet) {

      val tokens = tokenMap(field)
      val parsedTokens = if (field equals "category") {
        val clean = tokens.map(_.replaceAll("Category:", ""))
        val newTokens = clean.map(_.split(" ")).flatten
        newTokens
      } else if (field equals "fbtype") {
        val clean = tokens.map(_.replaceAll("/", " ").replaceAll("base", "").replaceAll("_", " "))
        val newTokens = clean.map(_.split(" ")).flatten
        newTokens

      } else {
        tokens
      }
      val biTerms = queryTokens.sliding(2, 1).toIndexedSeq

      val fieldLm = new LanguageModel(2)
      fieldLm.addDocument(parsedTokens, false)
      fieldLm.calculateProbabilities()


      val unigramWeight = if (queryTokens.size > 1) {
        0.85 / queryTokens.size
      } else {
        1.0d
      }

      var unigramProbWeighted = 0.0d
      var unigramProb = 0.0d

      val minProb = 0.0000001
      var numMatchingTerms = 0.0d
      var frequencySum = 0.0d

      for (q <- queryTokens) {


        // FIX with smoothing from background collection
        val te = fieldLm.getTermEntry(q)
        var prob = minProb
        if (te != null) {
          prob = te.getProbability()
          if (prob < minProb || prob > 1.0) {
            println("bad probability!")
            prob = minProb
          }
          numMatchingTerms += 1
          frequencySum += te.getFrequency
        }
        unigramProbWeighted += unigramWeight * Math.log(prob)
        unigramProb += 1.0 / queryTokens.size * Math.log(prob)
      }

      var odProbWeighted = 0.0d
      var odProb = 0.0d
      var uwProbWeighted = 0.0d
      var uwProb = 0.0d

      var phraseNumMatchingTerms = 0.0d
      var phraseFrequencySum = 0.0d

      var uwNumMatchingTerms = 0.0d
      var uwFrequencySum = 0.0d

      if (queryTokens.size > 1) {

        val odWeight = 0.05 / biTerms.size
        val uwWeight = 0.1 / biTerms.size

        for (bigram <- biTerms) {
          // FIX with smoothing from background collection
          val te = fieldLm.getTermEntry(bigram.mkString(" "))
          var prob = minProb
          if (te != null) {
            prob = te.getProbability()
            if (prob < 0.0 || prob > 1.0) {
              println("bad probability!")
              prob = minProb
            }
            phraseNumMatchingTerms += 1
            phraseFrequencySum += te.getFrequency
          }
          val logProb = Math.log(prob)
          val bigramProb = odWeight * logProb
          odProbWeighted += bigramProb
          odProb += 1.0 / biTerms.size * logProb
        }


        for (bigram <- biTerms) {
          // FIX with smoothing from background collection
          val t1 = fieldLm.getTermEntry(bigram(0))
          val t2 = fieldLm.getTermEntry(bigram(1))

          val p1 = if (t1 == null) {
            Array[Int]()
          } else {
            t1.getPositions().map(_.toInt).toArray
          }
          val p2 = if (t2 == null) {
            Array[Int]()
          } else {
            t2.getPositions().map(_.toInt).toArray
          }

          val hits = positions(8, Array(p1, p2))

          val count = hits.length

          var prob = minProb

          if (count > 0) {
            val baseProb = count.toDouble / fieldLm.getCollectionFrequency
            uwNumMatchingTerms += 1
            uwFrequencySum += count
            prob = baseProb
          }

          val logProb = Math.log(prob)
          uwProbWeighted += uwWeight * logProb
          uwProb += 1.0 / biTerms.size * logProb
        }
      }

      val fieldSdm = unigramProbWeighted + odProbWeighted + uwProbWeighted
      fieldProbMap.put(field + "-sdm", fieldSdm)
      fieldProbMap.put(field + "-od", odProb)
      fieldProbMap.put(field + "-uw", uwProb)
      fieldProbMap.put(field + "-uni", unigramProb)
      fieldProbMap.put(field + "-fieldLen", fieldLm.getCollectionFrequency)

      fieldProbMap.put(field + "-wordMatch", numMatchingTerms)
      fieldProbMap.put(field + "-wordProb", numMatchingTerms / queryTokens.size)
      fieldProbMap.put(field + "-wordFreqSum", frequencySum)

      val phraseDenom = if (queryTokens.size > 1) {
        (queryTokens.size - 1).toDouble
      } else {
        1.0d
      }
      fieldProbMap.put(field + "-numPhr", phraseNumMatchingTerms)
      fieldProbMap.put(field + "-PhrProb", phraseNumMatchingTerms / phraseDenom)
      fieldProbMap.put(field + "-PhrSum", phraseFrequencySum)

      fieldProbMap.put(field + "-numUW", uwNumMatchingTerms)
      fieldProbMap.put(field + "-UwProb", uwNumMatchingTerms / phraseDenom)
      fieldProbMap.put(field + "-UwSum", uwFrequencySum)

    }

    fieldProbMap
  }

  /**
   *
   * @param query
   * @param tokenMap
   * @param backgroundCollectionCounts collected with getStatistics(queryTerm)
   * @param backgroundBigramCollectionCounts collected with getStatistics(s"#ordered:1(${bigram.mkString(" ")})")
   * @param backgroundWindowBigramCollectionCounts collected with backgroundCollection.getStatistics(s"#unordered:8(${bigram.mkString(" ")})").nodeFrequency
   * @param mu
   * @return
   */
  def extractFeaturesWithCollectionSmoothing(query: String, tokenMap: Map[String, ListBuffer[String]],
                                             backgroundCollectionCounts:TermCollectionCounts,
                                             backgroundBigramCollectionCounts:TermCollectionCounts,
                                             backgroundWindowBigramCollectionCounts:TermCollectionCounts,
                                             mu:Int=1500): mutable.HashMap[String, Double] = {
    val queryTokens = TextNormalizer.normalizeFilterStop(query,StopWordList.isStopWord)
    val fieldProbMap = mutable.HashMap[String, Double]()

    for (field <- tokenMap.keySet) {

      val tokens = tokenMap(field)
      val parsedTokens = if (field equals "category") {
        val clean = tokens.map(_.replaceAll("Category:", ""))
        val newTokens = clean.map(_.split(" ")).flatten
        newTokens
      } else if (field equals "fbtype") {
        val clean = tokens.map(_.replaceAll("/", " ").replaceAll("base", "").replaceAll("_", " "))
        val newTokens = clean.map(_.split(" ")).flatten
        newTokens

      } else {
        tokens
      }
      val biTerms = queryTokens.sliding(2, 1).toIndexedSeq

      val fieldLm = new LanguageModel(2)
      fieldLm.addDocument(parsedTokens, false)
      fieldLm.calculateProbabilities()


      val unigramWeight = if (queryTokens.size > 1) {
        0.85 / queryTokens.size
      } else {
        1.0d
      }

      var unigramProbWeighted = 0.0d
      var unigramProb = 0.0d

      var numMatchingTerms = 0.0d
      var frequencySum = 0.0d

      val collLength = backgroundCollectionCounts._1
      for (q <- queryTokens) {
        val collFreq = backgroundCollectionCounts._2(q)._1

        val cf = if (collFreq == 0) {
          0.5 / collLength
        } else {
          collFreq.toDouble / collLength
        }

        val te = fieldLm.getTermEntry(q)
        val c = if (te != null) {

          numMatchingTerms += 1
          frequencySum += te.getFrequency

          te.getFrequency
        } else {
          0
        }
        val l = fieldLm.getCollectionFrequency

        val prob = dirichletSmoothedProb(l, c, mu, cf)


        unigramProbWeighted += unigramWeight * prob
        unigramProb += 1.0 / queryTokens.size * prob
      }

      var odProbWeighted = 0.0d
      var odProb = 0.0d
      var uwProbWeighted = 0.0d
      var uwProb = 0.0d

      var phraseNumMatchingTerms = 0.0d
      var phraseFrequencySum = 0.0d

      var uwNumMatchingTerms = 0.0d
      var uwFrequencySum = 0.0d

      if (queryTokens.size > 1) {

        val odWeight = 0.05 / biTerms.size
        val uwWeight = 0.1 / biTerms.size

        for (bigram <- biTerms) {

          val collFreq = backgroundBigramCollectionCounts._2(bigram.mkString(" "))._1

          val cf = if (collFreq == 0) {
            0.5 / collLength
          } else {
            collFreq.toDouble / collLength
          }

          val te = fieldLm.getTermEntry(bigram.mkString(" "))
          val c = if (te != null) {
            phraseNumMatchingTerms += 1
            phraseFrequencySum += te.getFrequency

            te.getFrequency
          } else {
            0
          }
          val l = fieldLm.getCollectionFrequency

          val prob = dirichletSmoothedProb(l, c, mu, cf)

          val bigramProb = odWeight * prob
          odProbWeighted += bigramProb
          odProb += 1.0 / biTerms.size * prob
        }


        for (bigram <- biTerms) {
          val t1 = fieldLm.getTermEntry(bigram(0))
          val t2 = fieldLm.getTermEntry(bigram(1))


          val collFreq = backgroundWindowBigramCollectionCounts._2(bigram.mkString(" "))._1

          val cf = if (collFreq == 0) {
            0.5 / collLength
          } else {
            collFreq.toDouble / collLength
          }


          val p1 = if (t1 == null) {
            Array[Int]()
          } else {
            t1.getPositions().map(_.toInt).toArray
          }
          val p2 = if (t2 == null) {
            Array[Int]()
          } else {
            t2.getPositions().map(_.toInt).toArray
          }

          val hits = positions(8, Array(p1, p2))

          val count = hits.length

          if (count > 0) {
            uwNumMatchingTerms += 1
            uwFrequencySum += count
          }

          val l = fieldLm.getCollectionFrequency
          val c = count

          val prob = dirichletSmoothedProb(l, c, mu, cf)
          uwProbWeighted += uwWeight * prob
          uwProb += 1.0 / biTerms.size * prob
        }
      }

      val fieldSdm = unigramProbWeighted + odProbWeighted + uwProbWeighted
      fieldProbMap.put(field + "-sdm", fieldSdm)
      fieldProbMap.put(field + "-od", odProb)
      fieldProbMap.put(field + "-uw", uwProb)
      fieldProbMap.put(field + "-uni", unigramProb)
      fieldProbMap.put(field + "-fieldLen", fieldLm.getCollectionFrequency)

      fieldProbMap.put(field + "-wordMatch", numMatchingTerms)
      fieldProbMap.put(field + "-wordProb", numMatchingTerms / queryTokens.size)
      fieldProbMap.put(field + "-wordFreqSum", frequencySum)

      val phraseDenom = if (queryTokens.size > 1) {
        (queryTokens.size - 1).toDouble
      } else {
        1.0d
      }
      fieldProbMap.put(field + "-numPhr", phraseNumMatchingTerms)
      fieldProbMap.put(field + "-PhrProb", phraseNumMatchingTerms / phraseDenom)
      fieldProbMap.put(field + "-PhrSum", phraseFrequencySum)

      fieldProbMap.put(field + "-numUW", uwNumMatchingTerms)
      fieldProbMap.put(field + "-UwProb", uwNumMatchingTerms / phraseDenom)
      fieldProbMap.put(field + "-UwSum", uwFrequencySum)

    }

    fieldProbMap
  }


  def dirichletSmoothedProb(l:Long, c:Long, mu:Int, cf:Double) : Double =  {
    val num = (c + (mu * cf))
    val den = (l + mu)
    val rawScore = scala.math.log((c + (mu * cf)) / (l + mu))
    val prob = scala.math.log((c + (mu * cf)) / (l + mu))
    prob
  }


  // from julien

  def positions(width: Int, termPositions: Array[Array[Int]]): ExtentArray = {
    val hits = new ExtentArray()
    val eArrays = for (pos: Array[Int] <- termPositions) yield {
      new ExtentArray(pos)
    }
    if (eArrays.exists(!_.hasNext)) return hits
    var break = false
    while (allHaveNext(eArrays) && !break) {
      // val (minPos, maxPos, minIdx) = updateMinMaxPos(eArrays)
      // was refactored into the following faster code:
      var minIdx = -1
      var min = Int.MaxValue
      var max = 0
      var j = 0
      while (j < eArrays.length) {
        val cur = eArrays(j).head()
        if (cur < min) {
          min = cur
          minIdx = j
        }
        if (cur > max) {
          max = cur
        }
        j += 1
      }

      // see if it fits
      if (max - min < width || width == -1) hits.add(min, max)

      // move all lower bound eArrays foward
      // moveMinForward(eArrays, minPos)
      if (eArrays(minIdx).hasNext) {
        eArrays(minIdx).next()
      } else {
        break = true
      }
    }
    hits
  }

  protected def allHaveNext(eArrays: Array[ExtentArray]): Boolean = {
    var j = 0
    while (j < eArrays.length) {
      val hasNext = eArrays(j).hasNext
      if (!hasNext) {
        return false
      }
      j += 1
    }
    return true
  }


  def exactFieldMatchMap(mentionName: String, terms:Seq[String], tags: Seq[GalagoTag]) = {
    val fields = tags

    val fieldsToCount = Set("stanf_anchor-exact", "anchor-exact", "title-exact", "redirect-exact", "fbname-exact")
    var fieldExactMatchSum = 0;
    val fieldMatchMap = new HashMap[String, Int]

    val normalizedQuery = TextNormalizer.normalizeText(mentionName)

    for (f <- fields) {
      if (fieldsToCount contains f.name) {
        val fieldValue = terms.subList(f.begin, f.end).mkString("")
        if (normalizedQuery.length() > 0 && (TextNormalizer.normalizeText(fieldValue) equals normalizedQuery)) {
          val curValue = fieldMatchMap.getOrElse(f.name, 0)
          fieldMatchMap += (f.name -> (curValue + 1))
        }
      }
    }
    fieldMatchMap
  }

}
