package edu.umass.ciir


import edu.umass.ciir.strepsi.distribution.Distribution

/**
 * Created by jdalton on 12/17/13.
 */
package object ede {

  type WikiTitle = String
  type Mention = String
  type LogScore = Double
  type StochasticEntityLink = (Distribution[WikiTitle], Mention)
  type EntityLink = (WikiTitle, Mention)
  type DocumentName = String

}
