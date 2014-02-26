package edu.umass.ciir.ede

import edu.umass.ciir.strepsi.trec.Judgment

/**
 * Created with IntelliJ IDEA.
 * User: jdalton
 * Date: 8/27/13
 * Time: 6:04 PM
 * To change this template use File | Settings | File Templates.
 */
package object elannotation {

  type QueryId = String
  type EntityId = String
  type QueryJudgmentSet = Map[QueryId, Seq[Judgment]]

  type WikiTitle = String
  type DocumentName = String
}
