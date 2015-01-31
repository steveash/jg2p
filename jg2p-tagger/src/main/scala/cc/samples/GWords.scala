package cc.samples

import cc.factorie.app.chain.Observation
import cc.factorie.util.Attr
import cc.factorie.variable._

/**
 * Collection of domain classes for the G2P problem
 * @author Steve Ash
 */
object GWords {

  // a GWord is a "word" that is a collection of graphemes
  class GWord(val wordSpaceString: String) extends Chain[GWord, Grapheme] with Attr {
    override def toString(): String = wordSpaceString
  }

  // a grapheme is a particular letter in the GWord
  class Grapheme(val x: String) extends Observation[Grapheme] with ChainLink[Grapheme, GWord] with Attr {
    override def string: String = x
  }

  object GraphemeFeaturesDomain extends CategoricalVectorDomain[String]

  class GraphemeFeatures(val grapheme: Grapheme) extends BinaryFeatureVectorVariable[String] {
    override def domain = GraphemeFeaturesDomain
  }

  object PhonemeLabelDomain extends CategoricalDomain[String] {
    this ++= Vector(
      "<EPS>"
    )
  }

  class Phoneme(val grapheme: Grapheme, val initialGuess: String) extends CategoricalVariable(initialGuess) {
    override def domain = PhonemeLabelDomain
  }

  class LabeledPhoneme(grapheme: Grapheme, initialGuess: String)
    extends Phoneme(grapheme, initialGuess) with CategoricalLabeling[String]

}
