package cc.samples

import java.util

import cc.factorie.infer.InferByBPChain
import cc.factorie.la._
import cc.factorie.model._
import cc.factorie.optimize.{L2Regularization, LBFGS, Trainer}
import cc.factorie.variable.HammingObjective
import cc.samples.GWords._
import cc.samples.InferChain.LikelihoodExample
import com.github.steveash.jg2p.align.Alignment
import com.github.steveash.jg2p.seq.SeqInputReader
import com.github.steveash.jg2p.util.TokenSeqUtil
import com.github.steveash.jg2p.util.TokenSeqUtil.convertShape
import com.google.common.base.{Stopwatch, Charsets}
import com.google.common.io.Resources
import org.apache.commons.lang3.StringUtils

import scala.collection.mutable.ListBuffer
import scala.collection.JavaConversions._

/**
 * @author Steve Ash
 */
object PhonemeChainExample {

//  trait SparsePlusEquals extends ArraySparseIndexedTensor {
//    override def +=(s:Double): Unit = {
//      var i = 0
//
//          while (i < __npos) {
//            __values(i) = math.exp(__values(i))
//            i += 1
//          }
//    }
//  }

  def makeModel(sentences:Seq[Seq[LabeledPhoneme]]): TemplateModel with Parameters = {
//    val pgWeights = new SparseIndexedTensor2()
    return new TemplateModel with Parameters {
      addTemplates(
        // bias terms for each label
        new DotTemplateWithStatistics1[Phoneme]() {
          println("Adding the phoneme template with " + PhonemeLabelDomain.dimensionSize)
//          val weights = Weights(new DenseTensor1(PhonemeLabelDomain.dimensionSize))
          val weights = Weights(new SparseIndexedTensor1(PhonemeLabelDomain.dimensionSize){ for(i <- 0 until dim1) update(i, 0.01) })
        },
        new DotTemplateWithStatistics2[Phoneme, GraphemeFeatures] {
          println("Adding the p->g template with g dize " + GraphemeFeaturesDomain.dimensionDomain)
          val weights = Weights(
//            new DenseTensor2(PhonemeLabelDomain.dimensionSize, GraphemeFeaturesDomain.dimensionSize))
            new SparseIndexedTensor2(PhonemeLabelDomain.dimensionSize, GraphemeFeaturesDomain.dimensionSize) {
              for(i <- 0 until dim1; j <- 0 until dim2) update(i, j, 0.01)
            })

          override def unroll1(p: Phoneme) = List(Factor(p, p.grapheme.attr[GraphemeFeatures]))

          override def unroll2(gf: GraphemeFeatures) = List(Factor(gf.grapheme.attr[Phoneme], gf))
        },
        new DotTemplateWithStatistics2[Phoneme, Phoneme] {
//          val weights = Weights(new DenseTensor2(PhonemeLabelDomain.dimensionSize, PhonemeLabelDomain.dimensionSize))
            val weights = Weights(new SparseIndexedTensor2(PhonemeLabelDomain.dimensionSize, PhonemeLabelDomain.dimensionSize) {
              for (i <- 0 until dim1; j <- 0 until dim2) {
                update(i, j, 0.01)
              }
            })

          // by convention we're going to do A-B in unroll1 and B-C in unroll2
          override def unroll1(p: Phoneme): Iterable[FactorType] = {
            if (p.grapheme.hasPrev) {
              return List(Factor(p.grapheme.prev.attr[Phoneme], p))
            } else {
              return Nil
            }
          }

          override def unroll2(p: Phoneme): Iterable[FactorType] = {
            if (p.grapheme.hasNext) {
              return List(Factor(p, p.grapheme.next.attr[Phoneme]))
            } else {
              return Nil
            }
          }

        }
      )
    }
  }

  def main(args: Array[String]): Unit = {
    implicit val random = new scala.util.Random(0)
    //    if (args.length != 1) {
    //      throw new Error("Usage: trainFile")
    //    }
    val file = "cmubad.2kA.align.txt"
    println("Before loading the domain sizes are p, g" + PhonemeLabelDomain.dimensionSize + ", " +
            GraphemeFeaturesDomain.dimensionSize)
    val alignGroups = new SeqInputReader()
      .readInput(Resources.asCharSource(Resources.getResource(file), Charsets.UTF_8))
    val aligns = alignGroups.flatMap(_.alignments)
    val gwords = aligns.take(250).map(a => {
      val gw = new GWord(a.getWordAsSpaceString)
      emitFeatures(a, gw)
      gw
    })
    println("After loading the domain sizes are p, g " + PhonemeLabelDomain.dimensionSize + ", " +
            GraphemeFeaturesDomain.dimensionSize)
    val wordPhones: Seq[Seq[LabeledPhoneme]] = gwords.map(_.asSeq.map(_.attr[LabeledPhoneme]))

    val model = makeModel(wordPhones);
    val watch = Stopwatch.createStarted()
    println("*** Starting training (#sentences=%d)".format(wordPhones.size))
    val examples = wordPhones.map(s => new LikelihoodExample(s, model, InferChainSum))

    val ex = wordPhones.get(0)
    val summ = InferChainSum.infer(ex, model)

    val opto = new LBFGS with L2Regularization
    opto.variance = 2.0

    println("Model param count " + model.parameters.length)
    Trainer.batchTrain(model.parameters, examples, optimizer = opto)
    watch.stop()
    println("After training, model param count " + model.parameters.length)

    println("*** Training took " + watch.toString)
    println("*** Starting inference (#sentences=%d)".format(wordPhones.size))
    wordPhones.foreach { variables => cc.factorie.infer.BP.inferChainMax(variables, model).setToMaximize(null)}

    println("test token accuracy=" + HammingObjective.accuracy(wordPhones.flatten))

  }

  def addNeighbor(gf: GraphemeFeatures, xs: util.List[String], i: Int, offset: Int, width: Int, moniker:String = "N", xform: String => String = identity) = {
    val maybe = TokenSeqUtil.getWindowFromStrings(xs, i, offset, width)
    if (maybe != null) {
      gf += moniker + "_" + offset + "_" + width + "_" + xform(maybe)
    }
  }

  def emitFeatures(a: Alignment, gw: GWord) {
    val xs = a.getAllXTokensAsList.map(_.toLowerCase)
    for (i <- 0 until a.getGraphones.size()) {
      val xy = a.getGraphones.get(i)
      val g = new Grapheme(xy.getLeft)
      val gf = new GraphemeFeatures(g)
      gf += "W=" + g.x  // the grapheme itself

      addNeighbor(gf, xs, i, -1, 1)
      addNeighbor(gf, xs, i, -2, 1)
      addNeighbor(gf, xs, i, -3, 1)
      addNeighbor(gf, xs, i, -2, 2)
      addNeighbor(gf, xs, i, 1, 1)
      addNeighbor(gf, xs, i, 2, 1)
      addNeighbor(gf, xs, i, 3, 1)

      addNeighbor(gf, xs, i, -5, 5, "S", convertShape)
      addNeighbor(gf, xs, i, -4, 4, "S", convertShape)
      addNeighbor(gf, xs, i, -3, 3, "S", convertShape)
      addNeighbor(gf, xs, i, -2, 2, "S", convertShape)
      addNeighbor(gf, xs, i, -1, 1, "S", convertShape)
      addNeighbor(gf, xs, i, 1, 1, "S", convertShape)
      addNeighbor(gf, xs, i, 1, 2, "S", convertShape)
      addNeighbor(gf, xs, i, 1, 3, "S", convertShape)
      addNeighbor(gf, xs, i, 1, 4, "S", convertShape)
      addNeighbor(gf, xs, i, 1, 5, "S", convertShape)

      g.attr += gf
      g.attr += new LabeledPhoneme(g, if (StringUtils.isBlank(xy.getRight)) "<EPS>"  else xy.getRight)
      gw += g
    }
  }
}
