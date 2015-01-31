package cc.samples

import cc.factorie.infer._
import cc.factorie.la.{Tensor, WeightsMapAccumulator}
import cc.factorie.model.{DotFamily, Family, Model}
import cc.factorie.optimize.Example
import cc.factorie.util.DoubleAccumulator
import cc.factorie.variable.{DiscreteVar, TargetAssignment, Var}

/**
 * @author Steve Ash
 */
object InferChainSum extends InferByBP {
  override def infer(variables: Iterable[DiscreteVar], model: Model, marginalizing: Summary): Summary = {
    if (marginalizing ne null) {
      throw new Error("Marginalizing case not yet implemented.")
    }
    InferChain.inferChainSum(variables.toSeq, model);
  }
}

object InferChain {
  // Works specifically on a linear-chain with factors Factor2[Label,Features], Factor1[Label] and Factor2[Label1,Label2]
  def inferChainSum(varying: Seq[DiscreteVar], model: Model): BPSummary = {
    val summary = BPSummary(varying, BPSumProductRing, model)
    varying.size match {
      case 0 => {}
      case 1 =>
        summary.bpFactors.foreach( edge => {
          edge.updateOutgoing()
        })
        summary.bpVariables.head.updateOutgoing()
      case _ =>
        // TODO There is a tricky dependency here: "varying" comes in order, and we are relying on the summary.bpFactors returning factors in chain order also!  Make this safer. -akm
        val obsBPFactors = summary.bpFactors.toSeq.filter(_.isInstanceOf[BPFactor1]).asInstanceOf[Seq[BPFactor1]]
          .toArray // this includes both Factor1[Label], Factor2[Label,Features]
      val markovBPFactors = summary.bpFactors.toSeq.filter(_.isInstanceOf[BPFactor2]).asInstanceOf[Seq[BPFactor2]]
          .toArray
        assert(obsBPFactors.size + markovBPFactors.size == summary.bpFactors.size)
        // assert(markovBPFactors.length < 2 || markovBPFactors.sliding(2).forall(fs => fs(0).edge2.bpVariable == fs(1).edge1.bpVariable)) // Make sure we got the Markov chain factors in order!
        // Send all messages from observations to labels in parallel
        obsBPFactors.foreach(edge => {
          edge.edge1.bpFactor.updateOutgoing()
        })
        // Send forward messages
        for (f <- markovBPFactors) {
          f.edge1.bpVariable.updateOutgoing(f
                                              .edge1) // send message from neighbor1 to factor // TODO Note that Markov factors must in sequence order!  Assert this somewhere!
          f.updateOutgoing(f.edge2) // send message from factor to neighbor2
        }
        // Send backward messages
        for (f <- markovBPFactors.reverse) {
          f.edge2.bpVariable.updateOutgoing(f.edge2) // send message from neighbor2 to factor
          f.updateOutgoing(f.edge1) // send message from factor to neighbor1
        }
        // Send messages out to obs factors so that they have the right logZ
        obsBPFactors.foreach(f => {
          f.edge1.bpVariable.updateOutgoing(f.edge1)
        })
    }
    summary
  }

  class LikelihoodExample[A <: Iterable[Var], B <: Model](labels: A, model: B, val infer: Infer[A, B]) extends Example {
    def accumulateValueAndGradient(value: DoubleAccumulator, gradient: WeightsMapAccumulator): Unit = {
      val summary = infer.infer(labels, model)
      if (value != null) {
        value.accumulate(-summary.logZ)
      }
      val factorMarginals = summary.factorMarginals
      for (factorMarginal <- factorMarginals) {
        factorMarginal.factor match {
          case factor: DotFamily#Factor if factor.family.isInstanceOf[DotFamily] =>
            val aStat = factor.assignmentStatistics(TargetAssignment)
            if (value != null) {
              val zvalue: Double = factor.statisticsScore(aStat)
              value.accumulate(zvalue)
            }
            if (gradient != null) {
              val tensorStats: Tensor = factorMarginal.tensorStatistics
              gradient.accumulate(factor.family.weights, aStat)
              gradient.accumulate(factor.family.weights, tensorStats, -1.0)
            }
          case factor: Family#Factor if !factor.family.isInstanceOf[DotFamily] =>
            if (value != null) {
              value.accumulate(factor.assignmentScore(TargetAssignment))
            }
        }
      }
    }
  }

}
