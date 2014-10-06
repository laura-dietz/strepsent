package edu.umass.ciir


import edu.umass.ciir.strepsi.distribution.Distribution

/**
 * Created by jdalton on 12/17/13.
 */
package object strepsent {

  type WikiTitle = String
  type Mention = String
  type LogScore = Double
  type StochasticEntityLink = (Distribution[WikiTitle], Mention)
  type EntityLink = (WikiTitle, Mention)
  type DocumentName = String

  type EntityId = String
  type Category = String
  type FreeBaseType = String
  type FeatureName = String
  type Term = String

  type WikiEntityId = EntityId
  type FreebaseEntityId = EntityId

}
