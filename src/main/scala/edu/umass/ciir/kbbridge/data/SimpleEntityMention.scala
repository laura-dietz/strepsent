package edu.umass.ciir.kbbridge.data

import edu.umass.ciir.kbbridge.data.NlpData.NlpXmlNerMention
import edu.umass.ciir.ede.elannotation.LinkedMention

/**
 * User: dietz
 * Date: 2/25/14
 * Time: 2:53 PM
 */
case class SimpleEntityMention(override val docId: String, override val entityType: String, override val mentionId: String,
override val entityName: String,  override val fullText:String, override val corefChain: Seq[NlpXmlNerMention] = Seq(), override val nerNeighbors :Seq[NlpXmlNerMention]=Seq(), override val groundTruth:String="", override val linkedMentions : Seq[LinkedMention] = Seq())
extends EntityMention(docId, entityType, mentionId, entityName, corefChain, groundTruth, nerNeighbors)
