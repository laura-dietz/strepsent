package edu.umass.ciir.kbbridge.data

import edu.umass.ciir.kbbridge.data.NlpData.NlpXmlNerMention
import edu.umass.ciir.ede.elannotation.LinkedMention

/**
 * User: jdalton
 * Date: 3/29/13
 */
abstract class EntityMention(val docId: String,
                             val entityType: String,
                             val mentionId: String,
                             val entityName: String,
                             val corefChain: Seq[NlpXmlNerMention] = Seq(),
                             val groundTruth: String,
                             val nerNeighbors: Seq[NlpXmlNerMention] = Seq(),
                             val linkedMentions: Seq[LinkedMention] = Seq()) {

  def fullText: String
}

